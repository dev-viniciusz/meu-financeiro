// Conversa com o backend (API Java): endereço, token de login e função de requisição.
(function () {
    
    const PRODUCTION_API_URL = "https://SEU-BACKEND.onrender.com";

    const isLocal = ["localhost", "127.0.0.1"].includes(window.location.hostname);
    const API_URL = isLocal ? "http://localhost:8080" : PRODUCTION_API_URL;
    const TOKEN_KEY = "fincontrol.token";

    function getToken() {
        return localStorage.getItem(TOKEN_KEY);
    }

    function isLoggedIn() {
        return Boolean(getToken());
    }

    function goToLogin() {
        window.location.href = "login.html";
    }

    // Chame nas páginas internas: se não há login, manda para a tela de login.
    function requireLogin() {
        if (!isLoggedIn()) goToLogin();
    }

    function logout() {
        localStorage.removeItem(TOKEN_KEY);
        goToLogin();
    }

    // Faz a chamada para a API. Devolve o JSON; em caso de erro, lança Error(mensagem).
    async function request(method, path, body) {
        const headers = {};
        if (body !== undefined) headers["Content-Type"] = "application/json";
        if (getToken()) headers.Authorization = `Bearer ${getToken()}`;

        let response;
        try {
            response = await fetch(API_URL + path, {
                method,
                headers,
                body: body === undefined ? undefined : JSON.stringify(body)
            });
        } catch {
            throw new Error("Não foi possível conectar ao servidor. Tente novamente.");
        }

        const data = await response.json().catch(() => ({}));

        // Token inválido/expirado em página interna: volta para o login.
        if (response.status === 401 && getToken()) {
            logout();
            throw new Error(data.error || "Sessão expirada.");
        }
        if (!response.ok) throw new Error(data.error || "Erro inesperado.");
        return data;
    }

    async function authenticate(path, payload) {
        const data = await request("POST", path, payload);
        localStorage.setItem(TOKEN_KEY, data.token);
        return data.profile;
    }

    window.Api = {
        request,
        isLoggedIn,
        requireLogin,
        logout,
        login: (email, password) => authenticate("/api/auth/login", { email, password }),
        register: (name, email, password) => authenticate("/api/auth/register", { name, email, password })
    };
})();
