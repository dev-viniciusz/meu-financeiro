(function () {
    const form = document.querySelector("#profile-form");
    const nameInput = document.querySelector("#profile-full-name");
    const emailInput = document.querySelector("#profile-email");
    const message = document.querySelector("#profile-message");
    async function loadProfile() {
        const profile = await FinanceStore.getProfile();
        nameInput.value = profile.name;
        emailInput.value = profile.email;
    }
    form.addEventListener("submit", async (event) => {
        event.preventDefault();
        message.classList.remove("positive");
        message.textContent = "";
        if (!form.checkValidity()) { form.reportValidity(); return; }
        try {
            await FinanceStore.updateProfile({ name: nameInput.value, email: emailInput.value });
            message.classList.add("positive");
            message.textContent = "Informações atualizadas.";
        } catch (error) { message.textContent = error.message; }
    });
    loadProfile().catch((error) => { message.textContent = error.message; });
})();
