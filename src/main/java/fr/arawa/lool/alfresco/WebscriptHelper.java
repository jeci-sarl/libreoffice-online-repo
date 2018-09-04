package fr.arawa.lool.alfresco;

import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptException;

public class WebscriptHelper {
    private WebscriptHelper() {
        // No Constructor
    }

    /**
     * Get Mandatory parameters from Map
     * 
     * @param templateArgs
     * @param header
     * @return
     * @throws WebScriptException
     */
    static public String getParam(Map<String, String> templateArgs, String header) throws WebScriptException {
        String value = templateArgs.get(header);
        assertParam(header, value);
        return value;
    }

    /**
     * Assert param is not null or empty
     * 
     * @param header
     *            Need only for log
     * @param param
     *            value tested
     * @throws WebScriptException
     */
    static public void assertParam(String header, String param) throws WebScriptException {
        if (StringUtils.isBlank(param)) {
            throw new WebScriptException(Status.STATUS_BAD_REQUEST, "No '" + header + "' parameter supplied");
        }
    }

    /**
     * Get parameter as Interger (Not Mandatory)
     * 
     * @param templateArgs
     * @param header
     * @return interger or null
     * @throws WebScriptException
     */
    static public Integer intergerValue(Map<String, String> templateArgs, String header) throws WebScriptException {
        final String strVal = templateArgs.get(header);
        if (strVal == null) {
            return null;
        }
        try {
            return Integer.parseInt(strVal);
        } catch (NumberFormatException e) {
            throw new WebScriptException(Status.STATUS_BAD_REQUEST,
                    "Parameter '" + header + "' is not a number = " + strVal);
        }
    }

}
