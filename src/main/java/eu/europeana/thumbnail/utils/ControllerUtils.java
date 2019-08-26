package eu.europeana.thumbnail.utils;

import javax.servlet.http.HttpServletResponse;
/**
 * Class containing a number of useful controller utilities (mainly for setting headers)
 *
 */
public class ControllerUtils {

        private static final String ALLOWED                 = "GET, HEAD";

        private ControllerUtils() {
            // to avoid instantiating this class
        }
        /**
         * Add the 'UTF-8' character encoding to the response
         *
         * @param response The response to add the encoding and headers to
         */
        public static void addResponseHeaders(HttpServletResponse response) {
            response.setCharacterEncoding("UTF-8");
            response.addHeader("Allow", ALLOWED);
        }
    }
