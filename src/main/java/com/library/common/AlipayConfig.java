package com.library.common;

public class AlipayConfig {

    public static final String APP_ID = "9021000164686727";
    public static final String PRIVATE_KEY = "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQDM9vGhCuoeAUezKV114gyholi+mFXoktUpsnx5I3mnu82Q3HYATht3e2Rwx1N9eAp6R/x2nhisSHg4ErBKK6XYCJfmvm7Y/KCvZLedIFeov5Dp1knR92+hIdc6Cd3ynLerErSirG45cwQkII8GZH7VHArSBexGR5kusiF1/qwusKe1RwFbDll7O8CkL8mSbZLVUDLw9q0BgBGqJvxX6Jh+0DKNAtnP+HgqD9iNwUImUh4OjNaUsGrgeHEumkuFNiiWkJFE/h2/M1N2sPBSUeZiqXgQr1zIfbUiIuEKBq4AWEJVxnLx9MqYHF9EoHspnPnLrLHPPm1vAWXELokKhGq3AgMBAAECggEAEKQoICQrt/3wnVpJbU3KKsFspSTnLLfnOLh2JLu9n+G+oflUV1gAIOZS1FlKrRA3AZCgrdzOMga8RAk4aP08PNsNwj1J8LIqNbNeuRIsK9ZJ5MJyQeruFE4UFHFCpPUWcHxXUDHhzSZKesht9F3qclERi9XRRi9//2uVDeh8gFnQHZwSdgMVL1zJTFTbXsQgFdkT19nobtUNJYAk0aGoQXlIbYEhJemggUILZdYdYYeqSulgxEkZGxZbMNNLbTmJW3BnRoUCQ9l0fJYzoYF8wITdEI6pS4W0HxfKFBSNvLGj0YJ1yvcV5Pvn4pY54i/3G4BDYeDCNQBDpyygrP682QKBgQD55eVdngJyQhgEnbr4mKN0TeRRgSbLq7QGUd0ZCTgpF/Y/mVJoBO5ChpnUnY5EK4D1FKoxtEtx60fltSpIn+6tHy7TGZqLp2Uq0CIxIfFhRb2rNIKJ5twYGiRXGBxSBAqjd+uGwqM/peZ71WB9fyDMjRyrj4ffcd+PQPIp40oL7QKBgQDR+Cu0F50kQUWSsSXIq65dv5uGjtsTk2d+KO7k5+aQ1TFS5V+WESYYDj3Whug3fWOF5jlCgVVE9vKQa8x4EdT/Y3mq+g+szTK+Rk1OF+5KnABdIB8SL9w65OCa+44v2wpJzHk+AGp4nMZGzwi8jTFNKvamvcIAQ7yg5UXnqbvkswKBgQDKke5U7hIjlqori8SVYf4V+FIdM6lELmKXQOMOOWf5/7+QnHOteFm/OacXL8S0Q+OcR20TL4h8xvGYJjwpdOOgGIf3fWjGziH3C5K76zzk0gjG8KDksmw1aXON2jCS103cZwhYTAzTOmNUfIpkEJqA1d6ov4evvHoxgL8M738FnQKBgAuw1rYbTnuTSs1lIaZvsLC1l3JBh/8qCOwkWVDTfKn5xNpJvS283LMgL98++bUFPUJaVDvYuaeyXEUgS20wCP0DS+XyMGc9saKYhXtgp7rtvrNws2ou0gNRdRQMB5mwh30ebm2+Vi9LA1sz8deVwPX7+2nDTQb4vory2RaSA80zAoGATfWCcdiGxZIyuhJS1AHYQ82C4BsWvXwr+5j6yG/CR6e9w0Car0U1GlRteANL+Ey2JtyS0HM+bkH53iCHQQZWs9abEfc/PPc1bTctn/YrejSC98ODVV1nuvvxHjOgwYgeBWTOO+vzfbXDhs+Mr1qxqDPndwKUfsYGqt2QXIArGs0=";
    public static final String ALIPAY_PUBLIC_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAnXLDY2jALPTXf6SFLTwQKhF2S4J1nqBKI2W+p2edWv2cB+aamhjSdHX5xdnIUVqBMKHmlIN4jmX+9ml+nopNlHN+VV8FREeN+QxSQvPibJfJ1C8rRt/69W3mC/dh8Z3E0NrEdBN2aprV3bZEfW87wY565qAbidGYNs9lPrPXJ3/Hzjqxr54pEqhtQpgus79JJez163C41RqPS4RuoUlYxYd6TgiDaMczvJVBir6IdJWS3bFH5TJuWdyjgd69Hi031PC1Yn69J8tJIKGddDe/RSRIg5ogYEy4hTNLTesrzL/Y8fBd2atDT/XIbCqxUw8LK2zePH2KGAn5iNkLjh/cCQIDAQAB";

    public static final String GATEWAY = "https://openapi-sandbox.dl.alipaydev.com/gateway.do";
    public static final String CHARSET = "UTF-8";
    public static final String SIGN_TYPE = "RSA2";
    public static final String FORMAT = "json";

    public static final String DOMAIN = "http://localhost:8080/library-ssm";

    public static final String RETURN_URL = DOMAIN + "/alipay/return";
    public static final String NOTIFY_URL = DOMAIN + "/alipay/notify";

    public static boolean isRealMode() {
        return APP_ID != null && !APP_ID.isEmpty()
                && PRIVATE_KEY != null && !PRIVATE_KEY.isEmpty()
                && ALIPAY_PUBLIC_KEY != null && !ALIPAY_PUBLIC_KEY.isEmpty();
    }
}