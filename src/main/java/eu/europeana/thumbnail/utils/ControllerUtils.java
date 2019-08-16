package eu.europeana.thumbnail.utils;

import org.apache.commons.lang3.StringUtils;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
/**
 * Class containing a number of useful controller utilities (mainly for setting headers)
 *
 */
public class ControllerUtils {

        private static final String ALLOWED                 = "GET, HEAD";
        private static final String ACCEPT                 = "Accept";

        private ControllerUtils() {
            // to avoid instantiating this class
        }

        /**
         * Extracts the format type from a request URL, e.g. JSON, JSON-LD, RDF, XML, etc.
         * @param request
         * @return String with request format, or null if no format information was found in the URL
         */
        public static String getRequestFormat(HttpServletRequest request) {
            String result = null;
            String uri = request.getRequestURI();
            if (uri.contains(".")) {
                result = uri.substring(uri.lastIndexOf('.')+1, uri.length());
            }
            return result;
        }

        /**
         * Extracts the value of the Accept header from the request URL, e.g. application/graphql
         * @param request
         * @return String with Accept header contents, or null if not found
         */
        public static String getRequestedMediaType(HttpServletRequest request) {
            if (StringUtils.isNotBlank(request.getHeader(ACCEPT))){
                return request.getHeader(ACCEPT);
            }
            return null;
        }

        public static String getRequestedContentType(HttpServletRequest request) {
            if (StringUtils.isNotBlank(request.getContentType())){
                return request.getContentType();
            }
            return null;
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

        public static boolean isHeadRequest(WebRequest request)
        {
            if(StringUtils.equalsIgnoreCase("HEAD", ((ServletWebRequest) request).getHttpMethod().toString()))
            {
                return true;
            }
            return false;
        }
    }

