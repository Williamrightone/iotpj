package com.willthx.common.model.auth;

/**
 * ThreadLocal 封裝器，管理 BFF 請求內的 {@link UserContext}。
 *
 * <p>使用規則：
 * <ul>
 *   <li>只有 {@code JwtAuthFilter} 呼叫 {@link #set(UserContext)}</li>
 *   <li>只有 UseCase 實作的 {@code execute(...)} 頂部呼叫 {@link #get()}，取一次後以參數傳遞</li>
 *   <li>業務服務不得匯入此類別</li>
 *   <li>{@link #clear()} 必須在 finally 區塊中呼叫，防止 ThreadLocal 洩漏</li>
 * </ul>
 */
public final class UserContextHolder {

    private static final ThreadLocal<UserContext> HOLDER = new ThreadLocal<>();

    private UserContextHolder() {}

    public static void set(UserContext ctx) {
        HOLDER.set(ctx);
    }

    public static UserContext get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
