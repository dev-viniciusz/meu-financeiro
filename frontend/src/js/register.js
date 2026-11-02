const form = document.querySelector(".register-form");
const inputFullName = document.querySelector("#full-name");
const inputEmail = document.querySelector("#register-email");
const inputPassword = document.querySelector("#register-password");
const fullNameError = document.querySelector("#full-name-error");
const emailError = document.querySelector("#register-email-error");
const passwordError = document.querySelector("#register-password-error");

form.addEventListener("submit", async (event) => {
    event.preventDefault();

    fullNameError.textContent = inputFullName.value.trim()
        ? ""
        : "Digite seu nome completo";
    emailError.textContent = inputEmail.value.trim() && inputEmail.validity.valid
        ? ""
        : "Digite um e-mail válido";
    passwordError.textContent = inputPassword.value.trim()
        ? ""
        : "Digite sua senha";

    if (fullNameError.textContent || emailError.textContent || passwordError.textContent) return;

    try {
        await Api.register(
            inputFullName.value.trim(),
            inputEmail.value.trim(),
            inputPassword.value
        );
        window.location.href = "dashboard.html";
    } catch (error) {
        // Ex.: e-mail já cadastrado, senha curta
        if (/senha/i.test(error.message)) passwordError.textContent = error.message;
        else emailError.textContent = error.message;
    }
});

inputFullName.addEventListener("input", () => {
    fullNameError.textContent = "";
});

inputEmail.addEventListener("input", () => {
    emailError.textContent = "";
});

inputPassword.addEventListener("input", () => {
    passwordError.textContent = "";
});
