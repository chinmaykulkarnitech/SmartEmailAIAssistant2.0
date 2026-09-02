console.log('Smart Email AI Assistant loaded');
let latestEmailAnalysis = null;
let selectedTone = 'professional';
let customInstruction = '';

function isVisible(element) {
    if (!element) return false;
    const style = window.getComputedStyle(element);
    const rect = element.getBoundingClientRect();
    return style.display !== 'none' && style.visibility !== 'hidden' && rect.width > 0 && rect.height > 0;
}

function findComposeToolbars() {
    const selectors = [
        '[role="dialog"] .btC',
        '[role="dialog"] .aDh',
        '[role="dialog"] [role="toolbar"]',
        '.btC',
        '.aDh'
    ];

    const seen = new Set();
    const result = [];

    for (const selector of selectors) {
        document.querySelectorAll(selector).forEach((toolbar) => {
            if (!seen.has(toolbar) && isVisible(toolbar)) {
                seen.add(toolbar);
                result.push(toolbar);
            }
        });
    }

    return result;
}

function createAiButton() {
    const button = document.createElement('button');
    button.className = 'ai-reply-button';
    button.type = 'button';
    button.textContent = '✨ AI Reply ▾';
    button.setAttribute('data-tooltip', 'Generate AI Reply');
    button.setAttribute('aria-label', 'Generate AI Reply');
    return button;
}

function getEmailContent() {
    const selectors = [
        '.a3s.aiL',
        '.a3s',
        '.gmail_quote'
    ];

    const candidates = [];

    for (const selector of selectors) {
        document.querySelectorAll(selector).forEach((element) => {
            if (isVisible(element)) {
                const text = element.innerText?.trim();
                if (text) candidates.push(text);
            }
        });
    }

    if (!candidates.length) return '';

    // Prefer the most substantial visible email body, while avoiding tiny UI strings.
    return candidates.sort((a, b) => b.length - a.length)[0];
}

function createToneMenu(button) {
    document.querySelectorAll('.ai-tone-menu').forEach((menu) => menu.remove());

    const menu = document.createElement('div');
    menu.className = 'ai-tone-menu';

    const title = document.createElement('div');
    title.className = 'ai-tone-title';
    title.textContent = 'AI Reply Settings';
    menu.appendChild(title);

    const toneLabel = document.createElement('div');
    toneLabel.className = 'ai-input-label';
    toneLabel.textContent = 'Reply Tone';
    menu.appendChild(toneLabel);

    const tones = [
        ['Professional', 'professional'],
        ['Friendly', 'friendly'],
        ['Formal', 'formal'],
        ['Casual', 'casual'],
        ['Concise', 'concise']
    ];

    const toneContainer = document.createElement('div');
    toneContainer.className = 'ai-tone-options';

    tones.forEach(([name, value]) => {
        const option = document.createElement('button');
        option.className = 'ai-tone-option';
        option.type = 'button';
        option.textContent = name;
        if (selectedTone === value) option.classList.add('selected');

        option.addEventListener('click', () => {
            selectedTone = value;
            toneContainer.querySelectorAll('.ai-tone-option').forEach((item) => item.classList.remove('selected'));
            option.classList.add('selected');
        });

        toneContainer.appendChild(option);
    });

    menu.appendChild(toneContainer);

    const instructionLabel = document.createElement('div');
    instructionLabel.className = 'ai-input-label';
    instructionLabel.textContent = 'Custom Instruction (Optional)';
    menu.appendChild(instructionLabel);

    const instructionInput = document.createElement('textarea');
    instructionInput.className = 'ai-instruction-input';
    instructionInput.placeholder = 'Example: Ask if Monday at 3 PM works for them.';
    instructionInput.rows = 3;
    instructionInput.value = customInstruction;
    instructionInput.addEventListener('input', () => {
        customInstruction = instructionInput.value;
    });
    menu.appendChild(instructionInput);

    const generateButton = document.createElement('button');
    generateButton.className = 'ai-generate-button';
    generateButton.type = 'button';
    generateButton.textContent = '✨ Generate Reply';
    generateButton.addEventListener('click', async () => {
        customInstruction = instructionInput.value.trim();
        menu.remove();
        await generateReply(button, customInstruction);
    });
    menu.appendChild(generateButton);

    document.body.appendChild(menu);

    // Position after the menu has been added so its real dimensions are available.
    const buttonRect = button.getBoundingClientRect();
    const menuRect = menu.getBoundingClientRect();
    const padding = 10;

    let left = Math.max(padding, Math.min(
        buttonRect.left,
        window.innerWidth - menuRect.width - padding
    ));

    let top = buttonRect.bottom + 6;
    if (top + menuRect.height > window.innerHeight - padding) {
        top = buttonRect.top - menuRect.height - 6;
    }
    top = Math.max(padding, top);

    menu.style.position = 'fixed';
    menu.style.left = `${left}px`;
    menu.style.top = `${top}px`;
    menu.style.visibility = 'visible';

    const closeOnOutsideClick = (event) => {
        if (!menu.contains(event.target) && event.target !== button) {
            menu.remove();
            document.removeEventListener('click', closeOnOutsideClick, true);
        }
    };
    setTimeout(() => document.addEventListener('click', closeOnOutsideClick, true), 0);
}

async function generateReply(button, instruction = '') {
    try {
        button.textContent = '⟳ Generating...';
        button.classList.add('generating');
        button.style.pointerEvents = 'none';

        const emailContent = getEmailContent();
        if (!emailContent) {
            throw new Error('No email content found. Open the email you want to reply to and try again.');
        }

        const result = await chrome.storage.local.get(['geminiApiKey']);
        const apiKey = result.geminiApiKey;

        if (!apiKey) {
            throw new Error('Please configure your Gemini API key in the extension settings.');
        }

        const response = await fetch('http://localhost:9090/api/email/generate', {
    method: 'POST',
    headers: {
        'Content-Type': 'application/json'
    },
    body: JSON.stringify({
        emailContent,
        tone: selectedTone,
        customInstruction: instruction,
        apiKey
    })
});

        const responseData = await response.json();

        if (!response.ok) {
            throw new Error(
                `API Request Failed: ${response.status} - ${JSON.stringify(responseData)}`
            );
        }

        latestEmailAnalysis = responseData;
        showEmailAnalysis(button, responseData);

        const generatedReply = responseData.reply?.trim();

        if (!generatedReply) {
            throw new Error('AI returned an empty reply.');
        }
        // Gmail's compose editor is contenteditable. Insert text and notify Gmail of the change.
        const composeBoxes = Array.from(document.querySelectorAll('[role="textbox"][contenteditable="true"]'))
            .filter(isVisible);
        const composeBox = composeBoxes[composeBoxes.length - 1];

        if (!composeBox) {
            throw new Error('Could not find the Gmail compose box.');
        }

        composeBox.focus();
        const inserted = document.execCommand('insertText', false, generatedReply);

        if (!inserted) {
            composeBox.textContent = generatedReply;
        }

        composeBox.dispatchEvent(new InputEvent('input', {
            bubbles: true,
            inputType: 'insertText',
            data: generatedReply
        }));

        console.log('AI reply inserted successfully.');
    } catch (error) {
        console.error('AI Reply Error:', error);
        alert(error?.message || 'Failed to generate reply. Please try again.');
    } finally {
        button.textContent = '✨ AI Reply ▾';
        button.classList.remove('generating');
        button.style.pointerEvents = 'auto';
    }
}

function injectButton(toolbar) {
    if (!toolbar || !isVisible(toolbar)) return;
    if (toolbar.querySelector('.ai-reply-button')) return;

    const button = createAiButton();
    button.addEventListener('click', (event) => {
        event.stopPropagation();
        createToneMenu(button);
    });

    toolbar.insertBefore(button, toolbar.firstChild);
    console.log('AI Reply button injected.');
}

function scanForComposeWindows() {
    findComposeToolbars().forEach(injectButton);
}

// Gmail is a SPA, so compose elements appear after the initial page load.
function startObserver() {
    scanForComposeWindows();

    const observer = new MutationObserver(() => {
        scanForComposeWindows();
    });

    observer.observe(document.body, { childList: true, subtree: true });

    // A lightweight fallback catches Gmail UI changes that do not produce useful mutations.
    setInterval(scanForComposeWindows, 1500);
}

if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', startObserver, { once: true });
} else {
    startObserver();
}

function showEmailAnalysis(button, analysis) {

    document.querySelectorAll('.ai-analysis-card')
        .forEach(card => card.remove());

    const card = document.createElement('div');
    card.className = 'ai-analysis-card';

    const title = document.createElement('div');
    title.className = 'ai-analysis-title';
    title.textContent = '🧠 Email Intelligence';

    card.appendChild(title);

    const intent = document.createElement('div');
    intent.className = 'ai-analysis-row';
    intent.innerHTML =
        `<strong>Intent:</strong> ${formatIntent(analysis.intent)}`;

    card.appendChild(intent);

    const priority = document.createElement('div');
    priority.className = 'ai-analysis-row';
    priority.innerHTML =
        `<strong>Priority:</strong> ${formatPriority(analysis.priority)}`;

    card.appendChild(priority);

    const sentiment = document.createElement('div');
    sentiment.className = 'ai-analysis-row';
    sentiment.innerHTML =
        `<strong>Sentiment:</strong> ${formatSentiment(analysis.sentiment)}`;

    card.appendChild(sentiment);

    const confidence = document.createElement('div');
    confidence.className = 'ai-analysis-row';

    const confidencePercent =
        Math.round((analysis.confidence || 0) * 100);

    confidence.innerHTML =
        `<strong>Confidence:</strong> ${confidencePercent}%`;

    card.appendChild(confidence);

    if (Array.isArray(analysis.keyPoints)
        && analysis.keyPoints.length > 0) {

        const keyTitle = document.createElement('div');
        keyTitle.className = 'ai-analysis-key-title';
        keyTitle.textContent = 'Key Points';

        card.appendChild(keyTitle);

        const list = document.createElement('ul');

        analysis.keyPoints.forEach(point => {

            const item = document.createElement('li');
            item.textContent = point;

            list.appendChild(item);
        });

        card.appendChild(list);
    }

    document.body.appendChild(card);

    const buttonRect = button.getBoundingClientRect();

    card.style.position = 'fixed';
    card.style.left = `${buttonRect.left}px`;
    card.style.top = `${buttonRect.bottom + 8}px`;
}

function formatIntent(intent) {

    if (!intent) return 'Unknown';

    return intent
        .toLowerCase()
        .replaceAll('_', ' ')
        .replace(/\b\w/g, char => char.toUpperCase());
}

function formatPriority(priority) {

    switch (priority) {

        case 'URGENT':
            return '🔴 Urgent';

        case 'HIGH':
            return '🟠 High';

        case 'MEDIUM':
            return '🟡 Medium';

        case 'LOW':
            return '🟢 Low';

        default:
            return '⚪ Unknown';
    }
}


function formatSentiment(sentiment) {

    switch (sentiment) {

        case 'POSITIVE':
            return '🟢 Positive';

        case 'NEGATIVE':
            return '🔴 Negative';

        case 'NEUTRAL':
            return '⚪ Neutral';

        case 'MIXED':
            return '🟡 Mixed';

        default:
            return 'Unknown';
    }
}