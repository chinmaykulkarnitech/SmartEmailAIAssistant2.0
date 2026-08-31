const apiKeyInput = document.getElementById("apiKey");
const saveButton = document.getElementById("saveButton");
const deleteButton = document.getElementById("deleteButton");
const status = document.getElementById("status");

// Load saved API key
chrome.storage.local.get(["geminiApiKey"], (result) => {

    if (result.geminiApiKey) {
        apiKeyInput.value = result.geminiApiKey;
    }

});

// Save API key
saveButton.addEventListener("click", () => {

    const apiKey = apiKeyInput.value.trim();

    if (!apiKey) {
        status.textContent = "Please enter your Gemini API key.";
        return;
    }

    chrome.storage.local.set(
        {
            geminiApiKey: apiKey
        },
        () => {

            status.textContent = "API key saved successfully.";

        }
    );

});

// Delete API key
deleteButton.addEventListener("click", () => {

    chrome.storage.local.remove(
        ["geminiApiKey"],
        () => {

            apiKeyInput.value = "";

            status.textContent = "API key deleted.";

        }
    );

});
