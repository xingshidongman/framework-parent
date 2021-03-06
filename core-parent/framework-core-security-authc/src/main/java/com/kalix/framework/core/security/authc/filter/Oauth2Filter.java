package com.kalix.framework.core.security.authc.filter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.kalix.framework.core.api.security.IShiroService;
import com.kalix.framework.core.security.authc.Constants;
import com.kalix.framework.core.security.authc.OAuth2ClientParams;
import com.kalix.framework.core.security.authc.Status;
import com.kalix.framework.core.util.HttpUtil;
import com.kalix.framework.core.util.OsgiUtil;
import org.apache.oltu.oauth2.common.OAuth;
import org.apache.oltu.oauth2.common.error.OAuthError;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.apache.oltu.oauth2.common.message.OAuthResponse;
import org.apache.oltu.oauth2.rs.response.OAuthRSResponse;
import org.apache.shiro.session.Session;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class Oauth2Filter implements Filter {
    private List<String> exceptUrlList = new ArrayList<>();
    private IShiroService shiroService;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        shiroService = OsgiUtil.waitForServices(IShiroService.class, null);
        String exceptUrls = filterConfig.getInitParameter("exceptUrls");
//        if (exceptUrls != null) {
//            for (String exceptUrl : exceptUrls.split(";")) {
//                exceptUrlList.add(exceptUrl);
//            }
//        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletResponse res = (HttpServletResponse) response;
        HttpServletRequest req = (HttpServletRequest) request;

        res.setHeader("Access-Control-Allow-Origin", "*");

        res.setHeader(
                "Access-Control-Allow-Headers",
                "User-Agent,Origin,Cache-Control,Content-type,Date,Server,withCredentials,AccessToken,JSESSIONID,access_token");

        res.setHeader("Access-Control-Allow-Credentials",
                "true");

        res.setHeader("Access-Control-Allow-Methods",
                "GET, POST, PUT, DELETE, OPTIONS, HEAD");

        res.setHeader("Access-Control-Max-Age", "1209600");

        res.setHeader("Access-Control-Expose-Headers", "access_token");

        res.setHeader("Access-Control-Request-Headers", "access_token");

        res.setHeader("Expires", "-1");

        res.setHeader("Cache-Control", "no-cache");

        res.setHeader("pragma", "no-cache");

        if (req.getMethod().equals("OPTIONS")) {
            res.setStatus(200);
            return;
        }

        try {
            boolean oauthUrl = true;
            //check the request url weather in the exceptURl or not
            for (String exceptUrl : exceptUrlList) {
                if (Pattern.compile(exceptUrl).matcher(req.getPathInfo()).matches()) {
                    oauthUrl = false;
                    break;
                }
            }

            if (oauthUrl) {
                //构建OAuth资源请求
                //OAuthAccessResourceRequest oauthQueryRequest = new OAuthAccessResourceRequest((HttpServletRequest) request, ParameterStyle.QUERY); // queryString 方式获取参数
                //OAuthAccessResourceRequest oauthHeaderRequest = new OAuthAccessResourceRequest((HttpServletRequest) request, ParameterStyle.HEADER); // 从HttpHead头中获取参数
//                Cookie cookies[] = req.getCookies();
                String accessToken = "";


//
//                for (int cIndex = cookies.length - 1; cIndex >= 0; --cIndex) {
//                    if (cookies[cIndex].getName().equals("access_token")) {
//                        accessToken = cookies[cIndex].getValue();
//                        break;
//                    }
//                }
                //验证Access Token
                String grant = req.getParameter("grant");
                accessToken = checkToken(req);
                if ("".equals(accessToken)) {
                    oAuthFailResponse(res);
                    return;
                }
                if (!"client".equals(grant)) {
                    if (shiroService.checkSessionTimeout()){
                        oAuthFailResponse(res);
                        return;
                    }
                }
                if (!checkAccessToken(accessToken)) {
                    if (!"client".equals(grant)) {
                        Session session = shiroService.getSession();
                        if (session != null) {
                            accessToken = getRefreshAccessToken(session);
                            res.setHeader("new_token", accessToken);
                            res.setHeader("Access-Control-Expose-Headers", "new_token");
                            System.out.println("刷新token:" + accessToken);
                        } else {
                            oAuthFailResponse(res);
                            return;
                        }
                    } else {
                        oAuthFailResponse(res);
                        return;
                    }
                }
            }

            chain.doFilter(request, response);
        }
        catch (OAuthSystemException e) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, "error trying to access oauth server", e);
        }
    }

    /**
     * oAuth认证失败时的输出
     *
     * @param res
     * @throws OAuthSystemException
     * @throws IOException
     */
    private void oAuthFailResponse(HttpServletResponse res) throws OAuthSystemException, IOException {
        OAuthResponse oauthResponse = OAuthRSResponse
                .errorResponse(HttpServletResponse.SC_UNAUTHORIZED)
                .setRealm(Constants.RESOURCE_SERVER_NAME)
                .setError(OAuthError.ResourceResponse.INVALID_TOKEN)
                .buildHeaderMessage();
        res.addHeader("Content-Type", "application/json; charset=utf-8");
        Gson gson = new GsonBuilder().create();
        res.addHeader(OAuth.HeaderType.WWW_AUTHENTICATE, oauthResponse.getHeader(OAuth.HeaderType.WWW_AUTHENTICATE));
        //PrintWriter writer = res.getWriter();
        ServletOutputStream sos = res.getOutputStream();

        sos.write(gson.toJson(getStatus(401, Constants.INVALID_ACCESS_TOKEN)).getBytes());
        sos.flush();
        sos.close();
        //writer.write(gson.toJson(getStatus(401,Constants.INVALID_ACCESS_TOKEN)));
        //writer.flush();
        //writer.close();
    }

    private String checkToken(HttpServletRequest req) {
        String accessToken = "";
        if (req.getHeader("AccessToken") != null) {
            accessToken = req.getHeader("AccessToken");
            return accessToken;
        }

        if (req.getParameter("AccessToken") != null) {
            accessToken = req.getParameter("AccessToken");
            return accessToken;
        }

        if (req.getHeader("access_token") != null) {
            accessToken = req.getHeader("access_token");
            return accessToken;
        }

        if (req.getParameter("access_token") != null) {
            accessToken = req.getParameter("access_token");
            return accessToken;
        }

        if (HttpUtil.getCookieByName(req, "access_token") != null) {
            accessToken = HttpUtil.getCookieByName(req, "access_token").getValue();
        }
        return accessToken;
    }

    /**
     * 验证accessToken
     *
     * @param accessToken
     * @return
     * @throws IOException
     */
    private boolean checkAccessToken(String accessToken) throws IOException {
        URL url = new URL(Constants.CHECK_ACCESS_CODE_URL + accessToken);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.disconnect();
        return HttpServletResponse.SC_OK == conn.getResponseCode();
    }

    private Status getStatus(int code, String msg) {
        Status status = new Status();
        status.setCode(code);
        status.setMsg(msg);
        return status;
    }

    @Override
    public void destroy() {

    }

    public List<String> getExceptUrlList() {
        return exceptUrlList;
    }

    public void setExceptUrlList(List<String> exceptUrlList) {
        this.exceptUrlList = exceptUrlList;
    }

    public String getRefreshAccessToken(Session session) {
        String refreshAccessToken = "";
        try {
            Map<String, Object> params = new LinkedHashMap<>();
            params.put("client_id", OAuth2ClientParams.CLIENT_ID);
            params.put("client_secret", OAuth2ClientParams.CLIENT_SECRET);
            params.put("grant_type", "refresh_token");
            String refreshCode = "";
            if (session != null) {
                refreshCode = (String)session.getAttribute("refresh_token");
            }
            params.put("refresh_token", refreshCode);
            params.put("redirect_uri", OAuth2ClientParams.OAUTH_SERVER_REDIRECT_URI);

            StringBuilder postStr = new StringBuilder();

            processUrl(params, postStr);

            byte[] postStrBytes = postStr.toString().getBytes("UTF-8");

            URL url = new URL(OAuth2ClientParams.OAUTH_SERVER_REFRESH_TOKEN_URL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            connection.setRequestProperty("Content-Length", String.valueOf(postStrBytes.length));
            connection.getOutputStream().write(postStrBytes);

            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            Gson gson = new GsonBuilder().create();
            Map<String, String> map = gson.fromJson(response.toString(), Map.class);
            if (map != null) {
                refreshAccessToken = map.get("access_token");
            }
            return refreshAccessToken;
        } catch(Exception e) {
            e.printStackTrace();
        }
        return refreshAccessToken;
    }

    private static void processUrl(Map<String, Object> params, StringBuilder postStr) throws UnsupportedEncodingException {
        for (Map.Entry<String, Object> param : params.entrySet()) {
            if (postStr.length() != 0) {
                postStr.append('&');
            }
            postStr.append(URLEncoder.encode(param.getKey(), "UTF-8"));
            postStr.append('=');
            postStr.append(URLEncoder.encode(String.valueOf(param.getValue()), "UTF-8"));
        }
    }
}
