package com.software.codetime.toolwindows.codetime.html;

import com.intellij.openapi.editor.colors.EditorColorsManager;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CssUtil {

    public static String updateBodyCss(String html) {
        Pattern pattern = Pattern.compile("(<body .*>)");
        Matcher matcher = pattern.matcher(html);
        matcher.find();
        if (matcher.groupCount() > 0) {
            String bodyTag = matcher.group(1);
            if (EditorColorsManager.getInstance().isDarkEditor()) {
                String darkBodyTag = "<body class=\"p-2 m-0\" style=\"height: 100vh; width: 100vw; background-color: rgba(39, 39, 42, 1);\">";
                html = html.substring(0, html.indexOf(bodyTag)) + darkBodyTag + html.substring(html.indexOf(bodyTag) + bodyTag.length());
            } else {
                String lightBodyTag = "<body class=\"p-2 m-0\" style=\"height: 100vh; width: 100vw; background-color: #FDFCFC;\">";
                html = html.substring(0, html.indexOf(bodyTag)) + lightBodyTag + html.substring(html.indexOf(bodyTag) + bodyTag.length());
            }
        }
        return html;
    }

    public static String getGlobalStyle() {
        if (EditorColorsManager.getInstance().isDarkEditor()) {
            return getDarkStyle();
        }
        return getLightStyle();
    }

    private static String getDarkStyle() {
        return "  <style type=\"text/css\">\n" +
                "    body { background-color: #2e2e2e; color: #fafafa; font-family: 'Inter', sans-serif; }\n" +
                "  </style>\n";
    }

    private static String getLightStyle() {
        return "  <style type=\"text/css\">\n" +
                "    body { font-family: 'Inter', sans-serif; }\n" +
                "  </style>\n";
    }
}
