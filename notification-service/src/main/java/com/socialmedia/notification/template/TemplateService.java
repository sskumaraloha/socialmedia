package com.socialmedia.notification.template;

import com.socialmedia.notification.exception.UnknownNotificationTemplateException;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import org.springframework.stereotype.Service;

/**
 * Localization via plain Java ResourceBundles (templates/notifications[_xx].properties)
 * rather than a templating engine - ResourceBundle already gives locale fallback
 * (an unsupported locale silently falls back to the base bundle) for free, and the
 * placeholder syntax is a simple {{name}} substitution rather than positional
 * MessageFormat args, which reads better for callers passing a named parameter map.
 */
@Service
public class TemplateService {

    private static final String BASE_NAME = "templates.notifications";

    public TemplateResult render(String templateKey, String locale, Map<String, String> params) {
        ResourceBundle bundle = ResourceBundle.getBundle(BASE_NAME, toLocale(locale));

        String subjectKey = templateKey + ".subject";
        String bodyKey = templateKey + ".body";
        if (!bundle.containsKey(subjectKey) || !bundle.containsKey(bodyKey)) {
            throw new UnknownNotificationTemplateException(templateKey);
        }

        return new TemplateResult(substitute(bundle.getString(subjectKey), params), substitute(bundle.getString(bodyKey), params));
    }

    private Locale toLocale(String locale) {
        if (locale == null || locale.isBlank()) {
            return Locale.ENGLISH;
        }
        try {
            return Locale.forLanguageTag(locale);
        } catch (IllegalArgumentException e) {
            return Locale.ENGLISH;
        }
    }

    private String substitute(String template, Map<String, String> params) {
        if (params == null || params.isEmpty()) {
            return template;
        }
        String result = template;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            result = result.replace("{{" + entry.getKey() + "}}", entry.getValue() == null ? "" : entry.getValue());
        }
        return result;
    }
}
