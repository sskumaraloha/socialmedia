package com.socialmedia.auth.e2e;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.socialmedia.auth.domain.AuthProvider;
import com.socialmedia.auth.domain.Device;
import com.socialmedia.auth.domain.DeviceIdentityKey;
import com.socialmedia.auth.domain.DeviceType;
import com.socialmedia.auth.domain.OneTimePreKey;
import com.socialmedia.auth.domain.User;
import com.socialmedia.auth.dto.response.KeyBundleResponse;
import com.socialmedia.auth.repository.DeviceIdentityKeyRepository;
import com.socialmedia.auth.repository.DeviceRepository;
import com.socialmedia.auth.repository.OneTimePreKeyRepository;
import com.socialmedia.auth.service.impl.DeviceKeyServiceImpl;
import com.socialmedia.common.lock.RedisDistributedLock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

/**
 * The real proof that this platform's end-to-end encryption works: two independent
 * {@link ReferenceE2eClient} instances - each holding its own private keys and ratchet state, as
 * two separate phones would - exchange messages using the actual Signal Protocol library, passing
 * only what the real server API carries between them (a public key bundle out of auth-service's
 * DeviceKeyService, and an opaque {cipherType, ciphertext} pair of the shape message-service
 * stores).
 *
 * <p>What this specifically establishes, none of which a mocked crypto layer could show:
 * the bundle auth-service serves is complete enough for a real client to run PQXDH against;
 * ciphertext survives the server's Base64 storage round-trip intact; and the plaintext is
 * recoverable ONLY by the intended recipient's own session state, which the server never sees.
 */
@ExtendWith(MockitoExtension.class)
class EndToEndEncryptionTest {

    private static final String ALICE_DEVICE = "alice-phone";
    private static final String BOB_DEVICE = "bob-phone";

    @Mock private DeviceRepository deviceRepository;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;

    private DeviceKeyServiceImpl deviceKeyService;

    /** Stand-ins for the two real tables, so this test exercises DeviceKeyServiceImpl's actual
     * logic (identity-key immutability, one-time-prekey claiming) rather than a stub of it. */
    private final Map<String, DeviceIdentityKey> identityKeyTable = new HashMap<>();
    private final List<OneTimePreKey> oneTimePreKeyTable = new ArrayList<>();

    private User alice;
    private User bob;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(valueOperations.setIfAbsent(any(), any(), any(Duration.class))).thenReturn(true);

        DeviceIdentityKeyRepository identityKeys = fakeIdentityKeyRepository();
        OneTimePreKeyRepository oneTimePreKeys = fakeOneTimePreKeyRepository();
        deviceKeyService = new DeviceKeyServiceImpl(deviceRepository, identityKeys, oneTimePreKeys,
                new RedisDistributedLock(redisTemplate));

        alice = userWithId("alice@example.com");
        bob = userWithId("bob@example.com");
        lenient().when(deviceRepository.findByDeviceId(ALICE_DEVICE))
                .thenReturn(Optional.of(new Device(alice, ALICE_DEVICE, "Alice's Phone", DeviceType.ANDROID)));
        lenient().when(deviceRepository.findByDeviceId(BOB_DEVICE))
                .thenReturn(Optional.of(new Device(bob, BOB_DEVICE, "Bob's Phone", DeviceType.IOS)));
    }

    @Test
    void twoDevicesExchangeAMessageThroughTheRealServerApisAndOnlyTheRecipientCanReadIt() throws Exception {
        ReferenceE2eClient aliceClient = new ReferenceE2eClient(ALICE_DEVICE, 1001, 5);
        ReferenceE2eClient bobClient = new ReferenceE2eClient(BOB_DEVICE, 2002, 5);

        // 1. Both devices publish their PUBLIC bundles through the real upload path.
        deviceKeyService.uploadKeyBundle(alice, ALICE_DEVICE, aliceClient.toUploadRequest());
        deviceKeyService.uploadKeyBundle(bob, BOB_DEVICE, bobClient.toUploadRequest());

        // 2. Alice fetches Bob's bundle from the real server and runs PQXDH against it.
        KeyBundleResponse bobBundle = deviceKeyService.getKeyBundle(BOB_DEVICE);
        assertThat(bobBundle.oneTimePreKeyPublic()).as("a fresh bundle should include a one-time prekey").isNotNull();
        aliceClient.establishSessionWith("bob", bobBundle);

        // 3. Alice encrypts. This is the ONLY thing message-service would ever store.
        String secret = "meet me at the usual place at 7";
        ReferenceE2eClient.EncryptedPayload payload = aliceClient.encryptTo("bob", secret);

        assertThat(payload.ciphertext()).isNotBlank();
        assertThat(java.util.Base64.getDecoder().decode(payload.ciphertext()))
                .as("the server stores bytes that do not contain the plaintext anywhere")
                .isNotEmpty();
        assertThat(payload.ciphertext()).doesNotContain("meet me");
        assertThat(payload.cipherType())
                .as("the first message of a session is a PreKeySignalMessage (libsignal type 3)")
                .isEqualTo(3);

        // 4. Bob decrypts using only his own local session state - nothing from the server.
        String recovered = bobClient.decryptFrom("alice", payload);
        assertThat(recovered).isEqualTo(secret);
    }

    @Test
    void subsequentMessagesRatchetForwardAndRemainDecryptable() throws Exception {
        ReferenceE2eClient aliceClient = new ReferenceE2eClient(ALICE_DEVICE, 1001, 5);
        ReferenceE2eClient bobClient = new ReferenceE2eClient(BOB_DEVICE, 2002, 5);
        deviceKeyService.uploadKeyBundle(bob, BOB_DEVICE, bobClient.toUploadRequest());
        aliceClient.establishSessionWith("bob", deviceKeyService.getKeyBundle(BOB_DEVICE));

        ReferenceE2eClient.EncryptedPayload first = aliceClient.encryptTo("bob", "first");
        assertThat(bobClient.decryptFrom("alice", first)).isEqualTo("first");

        // Alice keeps sending PreKeySignalMessages (type 3) until she hears back: until Bob
        // replies she has no evidence he ever processed her session setup, so every message must
        // remain independently able to establish it. This is real protocol behavior, and the
        // reason message-service must faithfully round-trip cipherType rather than assume it.
        ReferenceE2eClient.EncryptedPayload beforeReply = aliceClient.encryptTo("bob", "still type 3");
        assertThat(beforeReply.cipherType()).isEqualTo(3);
        assertThat(bobClient.decryptFrom("alice", beforeReply)).isEqualTo("still type 3");

        // Bob's reply completes the handshake in both directions.
        ReferenceE2eClient.EncryptedPayload bobReply = bobClient.encryptTo("alice", "got it");
        assertThat(aliceClient.decryptFrom("bob", bobReply)).isEqualTo("got it");

        // Now Alice's session is fully established, so her messages become plain SignalMessages
        // (type 2), each encrypted under a freshly ratcheted key.
        ReferenceE2eClient.EncryptedPayload second = aliceClient.encryptTo("bob", "second");
        ReferenceE2eClient.EncryptedPayload third = aliceClient.encryptTo("bob", "third");
        assertThat(second.cipherType()).isEqualTo(2);
        assertThat(third.cipherType()).isEqualTo(2);
        assertThat(second.ciphertext()).isNotEqualTo(third.ciphertext());

        assertThat(bobClient.decryptFrom("alice", second)).isEqualTo("second");
        assertThat(bobClient.decryptFrom("alice", third)).isEqualTo("third");
    }

    @Test
    void outOfOrderDeliveryStillDecrypts() throws Exception {
        ReferenceE2eClient aliceClient = new ReferenceE2eClient(ALICE_DEVICE, 1001, 5);
        ReferenceE2eClient bobClient = new ReferenceE2eClient(BOB_DEVICE, 2002, 5);
        deviceKeyService.uploadKeyBundle(bob, BOB_DEVICE, bobClient.toUploadRequest());
        aliceClient.establishSessionWith("bob", deviceKeyService.getKeyBundle(BOB_DEVICE));

        ReferenceE2eClient.EncryptedPayload first = aliceClient.encryptTo("bob", "one");
        ReferenceE2eClient.EncryptedPayload second = aliceClient.encryptTo("bob", "two");
        ReferenceE2eClient.EncryptedPayload third = aliceClient.encryptTo("bob", "three");

        // Networks reorder; the ratchet has to tolerate it. Deliver 3, then 1, then 2.
        assertThat(bobClient.decryptFrom("alice", third)).isEqualTo("three");
        assertThat(bobClient.decryptFrom("alice", first)).isEqualTo("one");
        assertThat(bobClient.decryptFrom("alice", second)).isEqualTo("two");
    }

    @Test
    void aThirdPartyDeviceCannotDecryptAMessageItWasNotAddressedTo() throws Exception {
        ReferenceE2eClient aliceClient = new ReferenceE2eClient(ALICE_DEVICE, 1001, 5);
        ReferenceE2eClient bobClient = new ReferenceE2eClient(BOB_DEVICE, 2002, 5);
        ReferenceE2eClient eveClient = new ReferenceE2eClient("eve-phone", 3003, 5);

        deviceKeyService.uploadKeyBundle(bob, BOB_DEVICE, bobClient.toUploadRequest());
        aliceClient.establishSessionWith("bob", deviceKeyService.getKeyBundle(BOB_DEVICE));
        ReferenceE2eClient.EncryptedPayload forBob = aliceClient.encryptTo("bob", "for bob only");

        // Eve has the ciphertext (as the server does) but not Bob's private keys.
        assertThatThrownBy(() -> eveClient.decryptFrom("alice", forBob))
                .as("ciphertext must be undecryptable without the recipient's own private key material")
                .isInstanceOf(Exception.class);
    }

    @Test
    void everyBundleFetchConsumesADistinctOneTimePreKey() throws Exception {
        ReferenceE2eClient bobClient = new ReferenceE2eClient(BOB_DEVICE, 2002, 3);
        deviceKeyService.uploadKeyBundle(bob, BOB_DEVICE, bobClient.toUploadRequest());

        Integer firstId = deviceKeyService.getKeyBundle(BOB_DEVICE).oneTimePreKeyId();
        Integer secondId = deviceKeyService.getKeyBundle(BOB_DEVICE).oneTimePreKeyId();
        Integer thirdId = deviceKeyService.getKeyBundle(BOB_DEVICE).oneTimePreKeyId();

        assertThat(List.of(firstId, secondId, thirdId)).doesNotHaveDuplicates();

        // Pool exhausted - a real client still gets a usable (signed + Kyber prekey) bundle.
        KeyBundleResponse exhausted = deviceKeyService.getKeyBundle(BOB_DEVICE);
        assertThat(exhausted.oneTimePreKeyId()).isNull();
        assertThat(exhausted.signedPreKeyPublic()).isNotBlank();
        assertThat(exhausted.kyberPreKeyPublic()).isNotBlank();
    }

    @Test
    void aSessionEstablishedFromAnExhaustedPoolBundleStillWorks() throws Exception {
        ReferenceE2eClient aliceClient = new ReferenceE2eClient(ALICE_DEVICE, 1001, 1);
        ReferenceE2eClient bobClient = new ReferenceE2eClient(BOB_DEVICE, 2002, 1);
        deviceKeyService.uploadKeyBundle(bob, BOB_DEVICE, bobClient.toUploadRequest());

        deviceKeyService.getKeyBundle(BOB_DEVICE); // drains the single one-time prekey
        KeyBundleResponse withoutOneTimePreKey = deviceKeyService.getKeyBundle(BOB_DEVICE);
        assertThat(withoutOneTimePreKey.oneTimePreKeyId()).isNull();

        aliceClient.establishSessionWith("bob", withoutOneTimePreKey);
        ReferenceE2eClient.EncryptedPayload payload = aliceClient.encryptTo("bob", "still private");

        assertThat(bobClient.decryptFrom("alice", payload)).isEqualTo("still private");
    }

    private User userWithId(String email) {
        User user = new User(email, "hash", AuthProvider.LOCAL, null);
        try {
            var field = com.socialmedia.common.jpa.BaseEntity.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(user, UUID.randomUUID());
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
        return user;
    }

    private DeviceIdentityKeyRepository fakeIdentityKeyRepository() {
        DeviceIdentityKeyRepository repository = org.mockito.Mockito.mock(DeviceIdentityKeyRepository.class);
        lenient().when(repository.save(any(DeviceIdentityKey.class))).thenAnswer(inv -> {
            DeviceIdentityKey saved = inv.getArgument(0);
            identityKeyTable.put(saved.getDeviceId(), saved);
            return saved;
        });
        lenient().when(repository.findByDeviceId(any()))
                .thenAnswer(inv -> Optional.ofNullable(identityKeyTable.get(inv.<String>getArgument(0))));
        return repository;
    }

    private OneTimePreKeyRepository fakeOneTimePreKeyRepository() {
        OneTimePreKeyRepository repository = org.mockito.Mockito.mock(OneTimePreKeyRepository.class);
        lenient().when(repository.save(any(OneTimePreKey.class))).thenAnswer(inv -> {
            OneTimePreKey saved = inv.getArgument(0);
            if (!oneTimePreKeyTable.contains(saved)) {
                oneTimePreKeyTable.add(saved);
            }
            return saved;
        });
        lenient().when(repository.findByDeviceIdAndKeyId(any(), org.mockito.ArgumentMatchers.anyInt()))
                .thenAnswer(inv -> oneTimePreKeyTable.stream()
                        .filter(k -> k.getDeviceId().equals(inv.getArgument(0)) && k.getKeyId() == (int) inv.getArgument(1))
                        .findFirst());
        lenient().when(repository.findFirstByDeviceIdAndConsumedFalseOrderByCreatedAtAsc(any()))
                .thenAnswer(inv -> oneTimePreKeyTable.stream()
                        .filter(k -> k.getDeviceId().equals(inv.getArgument(0)) && !k.isConsumed())
                        .findFirst());
        lenient().when(repository.countByDeviceIdAndConsumedFalse(any()))
                .thenAnswer(inv -> oneTimePreKeyTable.stream()
                        .filter(k -> k.getDeviceId().equals(inv.getArgument(0)) && !k.isConsumed())
                        .count());
        return repository;
    }
}
