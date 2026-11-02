const form = document.querySelector("form");
const inputEmail = document.querySelector("#email");
const inputPassword = document.querySelector("#password");
const emailError = document.querySelector("#email-error");
const passwordError = document.querySelector("#password-error");

form.addEventListener("submit", async (event) => {
    event.preventDefault();
    emailError.textContent = inputEmail.validity.valid ? "" : "Digite um email válido";
    passwordError.textContent = inputPassword.value.trim() ? "" : "Digite sua senha";
    if (emailError.textContent || passwordError.textContent) return;

    try {
        await Api.login(inputEmail.value.trim(), inputPassword.value);
        window.location.href = "dashboard.html";
    } catch (error) {
        passwordError.textContent = error.message;
    }
});

inputEmail.addEventListener("input", () => {
    emailError.textContent = "";
});

inputPassword.addEventListener("input", () => {
    passwordError.textContent = "";
});
