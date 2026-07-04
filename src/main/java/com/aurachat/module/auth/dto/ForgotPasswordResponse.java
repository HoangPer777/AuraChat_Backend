package com.aurachat.module.auth.dto;

public record ForgotPasswordResponse(
    String status,
    String message,
    String provider
) {
    public static ForgotPasswordResponse genericSuccess() {
        return new ForgotPasswordResponse(
            "SENT",
            "Nếu email tồn tại trong hệ thống, chúng tôi đã gửi liên kết xác thực.",
            null
        );
    }

    public static ForgotPasswordResponse linkSent() {
        return new ForgotPasswordResponse(
            "SENT",
            "Chúng tôi đã gửi liên kết xác thực đến email của bạn. Vui lòng kiểm tra hộp thư.",
            "LOCAL"
        );
    }

    public static ForgotPasswordResponse oauthAccount(String provider) {
        String providerLabel = switch (provider != null ? provider.toUpperCase() : "") {
            case "GOOGLE" -> "Google";
            case "FACEBOOK" -> "Facebook";
            default -> "mạng xã hội";
        };
        return new ForgotPasswordResponse(
            "OAUTH",
            "Tài khoản này đăng nhập bằng " + providerLabel + ". Vui lòng sử dụng phương thức đó để đăng nhập.",
            provider
        );
    }
}
