console.log('Chrome API:', chrome);
console.log('Chrome Storage:', chrome?.storage);
console.log('Chrome Runtime:', chrome?.runtime);
console.log('Smart Email AI Assistant loaded');
let latestEmailAnalysis = null;
let selectedTone = 'professional';
let selectedLanguage = 'auto';
let selectedReplyLength = 'medium';
let customInstruction = '';
let selectedIntelligenceLanguage = 'auto';
let lastGeneratedReply = '';


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



function getEmailThread() {
    const messageSelectors = [
        '.h7',
        '.gs',
        '.adn',
        '.a3s'
    ];

    const messages = [];
    const seen = new Set();

    for (const selector of messageSelectors) {
        document.querySelectorAll(selector).forEach((element) => {
            if (!isVisible(element)) return;

            const text = element.innerText?.trim();
            if (!text || text.length < 10) return;

            // Avoid collecting the same Gmail content multiple times
            const normalizedText = text
                .replace(/\s+/g, ' ')
                .trim();

            if (seen.has(normalizedText)) return;

            seen.add(normalizedText);

            messages.push({
                element,
                text
            });
        });
    }

    if (!messages.length) {
        return {
            threadContent: '',
            latestMessage: ''
        };
    }

    /*
     * Gmail can expose quoted/expanded messages through several
     * overlapping DOM elements. Remove obvious duplicates where
     * one message is completely contained inside another.
     */
    const uniqueMessages = messages.filter((message, index, array) => {
        return !array.some((other, otherIndex) => {
            if (index === otherIndex) return false;

            const current = message.text.replace(/\s+/g, ' ').trim();
            const otherText = other.text.replace(/\s+/g, ' ').trim();

            return (
                otherText.length > current.length &&
                otherText.includes(current)
            );
        });
    });

    /*
     * Sort messages according to their position in the Gmail DOM.
     * Gmail normally renders older messages before newer messages.
     */
    uniqueMessages.sort((a, b) => {
        if (a.element === b.element) return 0;

        const position = a.element.compareDocumentPosition(b.element);

        if (position & Node.DOCUMENT_POSITION_FOLLOWING) {
            return -1;
        }

        if (position & Node.DOCUMENT_POSITION_PRECEDING) {
            return 1;
        }

        return 0;
    });

    const threadContent = uniqueMessages
        .map((message, index) => {
            return `MESSAGE ${index + 1}\n${message.text}`;
        })
        .join('\n\n------------------------------\n\n');

    /*
     * The last message in the extracted conversation is treated
     * as the latest message that needs a reply.
     */
    const latestMessage =
        uniqueMessages[uniqueMessages.length - 1]?.text || '';

    return {
        threadContent,
        latestMessage
    };
}

function createToneMenu(button) {
    document.querySelectorAll('.ai-tone-menu').forEach((menu) => menu.remove());

    const menu = document.createElement('div');
    menu.className = 'ai-tone-menu';

    // ============================================================
    // TITLE
    // ============================================================

    const title = document.createElement('div');
    title.className = 'ai-tone-title';
    title.textContent = 'AI Reply Settings';
    menu.appendChild(title);


    // ============================================================
    // REPLY TONE
    // ============================================================

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

        if (selectedTone === value) {
            option.classList.add('selected');
        }

        option.addEventListener('click', () => {

            selectedTone = value;

            toneContainer
                .querySelectorAll('.ai-tone-option')
                .forEach((item) => {
                    item.classList.remove('selected');
                });

            option.classList.add('selected');
        });

        toneContainer.appendChild(option);
    });

    menu.appendChild(toneContainer);


    // ============================================================
    // REPLY LANGUAGE
    // ============================================================

    const languageLabel = document.createElement('div');
    languageLabel.className = 'ai-input-label';
    languageLabel.textContent = 'Reply Language';

    menu.appendChild(languageLabel);

    const languages = [
        ['Auto Detect', 'auto'],
        ['English', 'english'],
        ['Hindi', 'hindi'],
        ['Marathi', 'marathi'],
        ['Spanish', 'spanish'],
        ['French', 'french'],
        ['German', 'german']
    ];

    const languageContainer = document.createElement('div');
    languageContainer.className = 'ai-tone-options';

    languages.forEach(([name, value]) => {

        const option = document.createElement('button');

        option.className = 'ai-tone-option';
        option.type = 'button';
        option.textContent = name;

        if (selectedLanguage === value) {
            option.classList.add('selected');
        }

        option.addEventListener('click', () => {

            selectedLanguage = value;

            languageContainer
                .querySelectorAll('.ai-tone-option')
                .forEach((item) => {
                    item.classList.remove('selected');
                });

            option.classList.add('selected');
        });

        languageContainer.appendChild(option);
    });

    menu.appendChild(languageContainer);


    const intelligenceLanguageLabel = document.createElement('div');
    intelligenceLanguageLabel.className = 'ai-input-label';
    intelligenceLanguageLabel.textContent = 'Intelligence Language';
    menu.appendChild(intelligenceLanguageLabel);

    const intelligenceLanguages = [
        ['Auto Detect', 'auto'],
        ['English', 'english'],
        ['Hindi', 'hindi'],
        ['Marathi', 'marathi'],
        ['Spanish', 'spanish'],
        ['French', 'french'],
        ['German', 'german']
    ];

    const intelligenceLanguageContainer = document.createElement('div');
    intelligenceLanguageContainer.className = 'ai-tone-options';

    intelligenceLanguages.forEach(([name, value]) => {
        const option = document.createElement('button');
        option.className = 'ai-tone-option';
        option.type = 'button';
        option.textContent = name;

        if (selectedIntelligenceLanguage === value) {
            option.classList.add('selected');
        }

        option.addEventListener('click', () => {
            selectedIntelligenceLanguage = value;

            intelligenceLanguageContainer
                .querySelectorAll('.ai-tone-option')
                .forEach((item) => {
                    item.classList.remove('selected');
                });

            option.classList.add('selected');
        });

        intelligenceLanguageContainer.appendChild(option);
    });

    menu.appendChild(intelligenceLanguageContainer);


    // ============================================================
    // REPLY LENGTH
    // ============================================================

    const lengthLabel = document.createElement('div');
    lengthLabel.className = 'ai-input-label';
    lengthLabel.textContent = 'Reply Length';

    menu.appendChild(lengthLabel);

    const replyLengths = [
        ['Short', 'short'],
        ['Medium', 'medium'],
        ['Long', 'long']
    ];

    const lengthContainer = document.createElement('div');
    lengthContainer.className = 'ai-tone-options';

    replyLengths.forEach(([name, value]) => {

        const option = document.createElement('button');

        option.className = 'ai-tone-option';
        option.type = 'button';
        option.textContent = name;

        if (selectedReplyLength === value) {
            option.classList.add('selected');
        }

        option.addEventListener('click', () => {

            selectedReplyLength = value;

            lengthContainer
                .querySelectorAll('.ai-tone-option')
                .forEach((item) => {
                    item.classList.remove('selected');
                });

            option.classList.add('selected');
        });

        lengthContainer.appendChild(option);
    });

    menu.appendChild(lengthContainer);


    // ============================================================
    // CUSTOM INSTRUCTION
    // ============================================================

    const instructionLabel = document.createElement('div');
    instructionLabel.className = 'ai-input-label';
    instructionLabel.textContent = 'Custom Instruction (Optional)';

    menu.appendChild(instructionLabel);

    const instructionInput = document.createElement('textarea');

    instructionInput.className = 'ai-instruction-input';

    instructionInput.placeholder =
        'Example: Ask if Monday at 3 PM works for them.';

    instructionInput.rows = 3;

    instructionInput.value = customInstruction;

    instructionInput.addEventListener('input', () => {
        customInstruction = instructionInput.value;
    });

    menu.appendChild(instructionInput);


    // ============================================================
    // GENERATE BUTTON
    // ============================================================

    const generateButton = document.createElement('button');

    generateButton.className = 'ai-generate-button';

    generateButton.type = 'button';

    generateButton.textContent = '✨ Generate Reply';

    generateButton.addEventListener('click', async () => {

        customInstruction =
            instructionInput.value.trim();

        menu.remove();

        await generateReply(
            button,
            customInstruction
        );
    });

    menu.appendChild(generateButton);


    // ============================================================
    // ADD MENU TO PAGE
    // ============================================================

    document.body.appendChild(menu);


    // ============================================================
    // POSITION MENU
    // ============================================================

    const buttonRect =
        button.getBoundingClientRect();

    const menuRect =
        menu.getBoundingClientRect();

    const padding = 10;

    let left = Math.max(
        padding,
        Math.min(
            buttonRect.left,
            window.innerWidth -
            menuRect.width -
            padding
        )
    );

    let top =
        buttonRect.bottom + 6;

    if (
        top + menuRect.height >
        window.innerHeight - padding
    ) {
        top =
            buttonRect.top -
            menuRect.height -
            6;
    }

    top = Math.max(
        padding,
        top
    );

    menu.style.position = 'fixed';

    menu.style.left =
        `${left}px`;

    menu.style.top =
        `${top}px`;

    menu.style.visibility =
        'visible';


    // ============================================================
    // CLOSE WHEN CLICKING OUTSIDE
    // ============================================================

    const closeOnOutsideClick = (event) => {

        if (
            !menu.contains(event.target) &&
            event.target !== button
        ) {

            menu.remove();

            document.removeEventListener(
                'click',
                closeOnOutsideClick,
                true
            );
        }
    };

    setTimeout(() => {

        document.addEventListener(
            'click',
            closeOnOutsideClick,
            true
        );

    }, 0);
}
function insertGeneratedReply(composeBox, generatedReply) {

    const currentText =
        composeBox.innerText?.trim() || '';

    const previousGeneratedReply =
        lastGeneratedReply.trim();

    composeBox.focus();

    /*
     * If the compose box contains exactly
     * the reply generated previously by AI,
     * replace it.
     */
    if (
        previousGeneratedReply &&
        currentText === previousGeneratedReply
    ) {

        document.execCommand(
            'selectAll',
            false,
            null
        );

        document.execCommand(
            'insertText',
            false,
            generatedReply
        );

    } else {

        /*
         * The user has changed the compose content.
         * Do NOT delete their text.
         *
         * Insert the new reply at the current cursor.
         */
        document.execCommand(
            'insertText',
            false,
            generatedReply
        );
    }

    composeBox.dispatchEvent(
        new InputEvent('input', {
            bubbles: true,
            inputType: 'insertText',
            data: generatedReply
        })
    );
}
async function generateReply(button, instruction = '') {
    try {
        button.textContent = '⟳ Generating...';
        button.classList.add('generating');
        button.style.pointerEvents = 'none';

        const emailThread = getEmailThread();

        if (!emailThread.threadContent) {
            throw new Error(
                'No email conversation found. Open the email thread and try again.'
            );
        }

        console.log('AI Email Thread:', emailThread);

        console.log('Selected Tone:', selectedTone);
        console.log('Selected Language:', selectedLanguage);
        console.log('Selected Reply Length:', selectedReplyLength);

        const result = await chrome.storage.local.get(['geminiApiKey']);
        const apiKey = result.geminiApiKey;

        if (!apiKey) {
            throw new Error(
                'Please configure your Gemini API key in the extension settings.'
            );
        }

        const response = await fetch(
            'http://localhost:9090/api/email/generate',
            {
                method: 'POST',

                headers: {
                    'Content-Type': 'application/json'
                },

                body: JSON.stringify({
                    threadContent: emailThread.threadContent,
                    latestMessage: emailThread.latestMessage,

                    tone: selectedTone,
                    language: selectedLanguage,
                    intelligenceLanguage: selectedIntelligenceLanguage,
                    replyLength: selectedReplyLength,

                    customInstruction: instruction,

                    apiKey: apiKey
                })
            }
        );

        const responseData = await response.json();

        if (!response.ok) {
            throw new Error(
                `API Request Failed: ${response.status} - ${JSON.stringify(responseData)}`
            );
        }

        latestEmailAnalysis = responseData;

        console.log('========== AI RESPONSE DEBUG ==========');
        console.log('FULL AI RESPONSE:', JSON.stringify(responseData, null, 2));
        console.log('deadlineDetected:', responseData.deadlineDetected);
        console.log('deadline:', responseData.deadline);
        console.log('deadlineDescription:', responseData.deadlineDescription);
        console.log('========================================');
        const generatedReply = responseData.reply?.trim();

        if (!generatedReply) {
            throw new Error(
                'AI returned an empty reply.'
            );
        }

        showEmailAnalysis(button, responseData);

        // Gmail's compose editor is contenteditable.
        const composeBoxes = Array.from(
            document.querySelectorAll(
                '[role="textbox"][contenteditable="true"]'
            )
        ).filter(isVisible);

        const composeBox =
            composeBoxes[composeBoxes.length - 1];

        if (!composeBox) {
            throw new Error(
                'Could not find the Gmail compose box.'
            );
        }

        insertGeneratedReply(
            composeBox,
            generatedReply
        );

        lastGeneratedReply = generatedReply;

        console.log(
            'AI reply inserted successfully.'
        );
    } catch (error) {

        console.error(
            'AI Reply Error:',
            error
        );

        alert(
            error?.message ||
            'Failed to generate reply. Please try again.'
        );

    } finally {

        button.textContent =
            '✨ AI Reply ▾';

        button.classList.remove(
            'generating'
        );

        button.style.pointerEvents =
            'auto';
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

// Gmail is a SPA, so compose elements appear after
// the initial page load.

// ============================================================
// EMAIL INTELLIGENCE CLEANUP
// ============================================================

function removeEmailAnalysis() {
    document.querySelectorAll('.ai-analysis-card')
        .forEach(card => card.remove());

    latestEmailAnalysis = null;
}


// Remove the AI tone menu if it is still open.
function removeAiToneMenu() {
    document.querySelectorAll('.ai-tone-menu')
        .forEach(menu => menu.remove());
}


function getIntelligenceLabels(language) {

    const labels = {
        english: {
            intent: 'Intent',
            priority: 'Priority',
            sentiment: 'Sentiment',
            confidence: 'Confidence',
            keyPoints: 'Key Points',
            replyTranslation: 'Reply Translation',
            intelligence: '🧠 Email Intelligence'
        },

        hindi: {
            intent: 'इरादा',
            priority: 'प्राथमिकता',
            sentiment: 'भावना',
            confidence: 'विश्वास',
            keyPoints: 'मुख्य बिंदु',
            replyTranslation: 'उत्तर का अनुवाद',
            intelligence: '🧠 ईमेल इंटेलिजेंस'
        },

        marathi: {
            intent: 'हेतू',
            priority: 'प्राधान्य',
            sentiment: 'भावना',
            confidence: 'विश्वासार्हता',
            keyPoints: 'महत्त्वाचे मुद्दे',
            replyTranslation: 'उत्तराचे भाषांतर',
            intelligence: '🧠 ईमेल इंटेलिजन्स'
        },

        spanish: {
            intent: 'Intención',
            priority: 'Prioridad',
            sentiment: 'Sentimiento',
            confidence: 'Confianza',
            keyPoints: 'Puntos clave',
            replyTranslation: 'Traducción de la respuesta',
            intelligence: '🧠 Inteligencia del correo'
        },

        french: {
            intent: 'Intention',
            priority: 'Priorité',
            sentiment: 'Sentiment',
            confidence: 'Confiance',
            keyPoints: 'Points clés',
            replyTranslation: 'Traduction de la réponse',
            intelligence: '🧠 Intelligence de l’e-mail'
        },

        german: {
            intent: 'Absicht',
            priority: 'Priorität',
            sentiment: 'Stimmung',
            confidence: 'Konfidenz',
            keyPoints: 'Wichtige Punkte',
            replyTranslation: 'Antwortübersetzung',
            intelligence: '🧠 E-Mail-Intelligenz'
        }
    };

    return labels[language] || labels.english;
}

// ============================================================
// EMAIL INTELLIGENCE PANEL
// ============================================================


function showEmailAnalysis(button, analysis) {

    // Remove any previous analysis before showing new one.
    removeEmailAnalysis();

    const card = document.createElement('div');
    card.className = 'ai-analysis-card';
    const displayLanguage =
        selectedIntelligenceLanguage === 'auto'
            ? analysis.intelligenceLanguage
            : selectedIntelligenceLanguage;

    const intelligenceLabels =
        getIntelligenceLabels(displayLanguage);
    const title = document.createElement('div');
    title.className = 'ai-analysis-title';
    title.textContent = '🧠 Email Intelligence';

    card.appendChild(title);


    // -------------------------
    // Intent
    // -------------------------

    const intent = document.createElement('div');
    intent.className = 'ai-analysis-row';

    intent.innerHTML =
        `<strong>${intelligenceLabels.intent}:</strong> ${analysis.intentLabel || formatIntent(analysis.intent)}`
    card.appendChild(intent);


    // -------------------------
    // Priority
    // -------------------------

    const priority = document.createElement('div');
    priority.className = 'ai-analysis-row';

    priority.innerHTML =
        `<strong>${intelligenceLabels.priority}:</strong> ${analysis.priorityLabel || formatPriority(analysis.priority)}`
    card.appendChild(priority);


    // -------------------------
    // Sentiment
    // -------------------------

    const sentiment = document.createElement('div');
    sentiment.className = 'ai-analysis-row';

    sentiment.innerHTML =
        `<strong>${intelligenceLabels.sentiment}:</strong> ${analysis.sentimentLabel || formatSentiment(analysis.sentiment)}`
    card.appendChild(sentiment);


    // -------------------------
    // Confidence
    // -------------------------

    const confidence = document.createElement('div');
    confidence.className = 'ai-analysis-row';

    const confidencePercent =
        Math.round((analysis.confidence || 0) * 100);

    confidence.innerHTML =
        `<strong>${intelligenceLabels.confidence}:</strong> ${confidencePercent}%`
    card.appendChild(confidence);


    // -------------------------
    // Key Points
    // -------------------------

    if (
        Array.isArray(analysis.keyPoints) &&
        analysis.keyPoints.length > 0
    ) {

        const keyTitle = document.createElement('div');
        keyTitle.className = 'ai-analysis-key-title';
        keyTitle.textContent = intelligenceLabels.keyPoints;

        card.appendChild(keyTitle);

        const list = document.createElement('ul');

        analysis.keyPoints.forEach(point => {

            const item = document.createElement('li');
            item.textContent = point;

            list.appendChild(item);
        });

        card.appendChild(list);
    }

    if (analysis.replyTranslation?.trim()) {

        const translationTitle = document.createElement('div');
        translationTitle.className =
            'ai-analysis-translation-title';

        translationTitle.textContent =
            intelligenceLabels.replyTranslation;
        card.appendChild(translationTitle);

        const translation = document.createElement('div');
        translation.className = 'ai-reply-translation';
        translation.textContent =
            analysis.replyTranslation.trim();

        card.appendChild(translation);
    }

    const regenerateButton = document.createElement('button');

    regenerateButton.className = 'ai-regenerate-button';
    regenerateButton.textContent = '↻ Regenerate Reply';

    regenerateButton.addEventListener('click', async () => {
        regenerateButton.textContent = '⟳ Regenerating...';
        regenerateButton.disabled = true;

        try {
            await generateReply(button, customInstruction);
        } finally {
            regenerateButton.textContent = '↻ Regenerate Reply';
            regenerateButton.disabled = false;
        }
    });
    card.appendChild(regenerateButton);

// Position panel below AI Reply button.
    const buttonRect = button.getBoundingClientRect();

    card.style.position = 'fixed';
    card.style.left = `${buttonRect.left}px`;
    card.style.top = `${buttonRect.bottom + 8}px`;

    // Action Detection
    const actionSection = document.createElement('div');

    actionSection.className = 'ai-action-section';

    const actionTitle = document.createElement('div');

    actionTitle.className = 'ai-action-title';
    actionTitle.textContent = '✅ Action Detection';

    actionSection.appendChild(actionTitle);

    const actionRequired = document.createElement('div');

    actionRequired.className = 'ai-action-row';

    actionRequired.innerHTML = `
    <strong>Action Required:</strong>
    ${analysis.actionRequired ? 'Yes' : 'No'}
`;

    actionSection.appendChild(actionRequired);

    if (analysis.actionRequired) {

        const actionText = document.createElement('div');

        actionText.className = 'ai-action-row';

        actionText.innerHTML = `
        <strong>Action:</strong>
        ${analysis.action || 'None'}
    `;

        actionSection.appendChild(actionText);

        const actionStatus = document.createElement('div');

        actionStatus.className = 'ai-action-row';

        actionStatus.innerHTML = `
        <strong>Status:</strong>
        ${analysis.actionStatus || 'NONE'}
    `;

        actionSection.appendChild(actionStatus);
    }

    card.appendChild(actionSection);

// Deadline Detection
    if (analysis.deadlineDetected) {

        const deadlineSection = document.createElement('div');

        deadlineSection.className = 'ai-deadline-section';

        const deadlineTitle = document.createElement('div');

        deadlineTitle.className = 'ai-deadline-title';
        deadlineTitle.textContent = '📅 Deadline Detection';

        deadlineSection.appendChild(deadlineTitle);

        const deadlineDate = document.createElement('div');

        deadlineDate.className = 'ai-deadline-row';

        deadlineDate.innerHTML = `
        <strong>Deadline:</strong>
        ${analysis.deadline || 'Not specified'}
    `;

        deadlineSection.appendChild(deadlineDate);

        const deadlineDescription = document.createElement('div');

        deadlineDescription.className = 'ai-deadline-row';

        deadlineDescription.innerHTML = `
        <strong>For:</strong>
        ${analysis.deadlineDescription || 'Not specified'}
    `;

        deadlineSection.appendChild(deadlineDescription);

        card.appendChild(deadlineSection);

        const reminderButton = document.createElement('button');

        reminderButton.className = 'ai-reminder-button';
        reminderButton.textContent = '🔔 Remind me';

        reminderButton.addEventListener('click', () => {
            console.log('Reminder button clicked');
        });

        card.appendChild(reminderButton);
    }
// Add the fully constructed panel to Gmail.
    document.body.appendChild(card);
}

// ============================================================
// SEND BUTTON CLEANUP
// ============================================================

function setupSendButtonCleanup() {

    const observer = new MutationObserver(() => {

        const sendButtons = document.querySelectorAll(
            '[role="button"][data-tooltip*="Send"],' +
            '[role="button"][aria-label*="Send"],' +
            '.gU.Up'
        );


        sendButtons.forEach((sendButton) => {

            // Prevent attaching multiple listeners.
            if (
                sendButton.dataset.aiCleanupAttached === 'true'
            ) {
                return;
            }


            sendButton.dataset.aiCleanupAttached = 'true';


            sendButton.addEventListener('click', () => {

                console.log(
                    'Email sent. Cleaning AI UI.'
                );


                // Gmail needs a small amount of time
                // to process the send action.
                setTimeout(() => {

                    removeEmailAnalysis();
                    removeAiToneMenu();
                    lastGeneratedReply = '';

                }, 500);

            });

        });

    });


    observer.observe(document.body, {
        childList: true,
        subtree: true
    });
}


// ============================================================
// GMAIL NAVIGATION CLEANUP
// ============================================================

function setupNavigationCleanup() {

    let lastUrl = location.href;


    setInterval(() => {

        if (location.href !== lastUrl) {

            lastUrl = location.href;


            console.log(
                'Gmail conversation changed. Cleaning old AI UI.'
            );


            removeEmailAnalysis();
            removeAiToneMenu();
            lastGeneratedReply = '';

        }

    }, 500);
}


// ============================================================
// FORMAT FUNCTIONS
// ============================================================

function formatIntent(intent) {

    if (!intent) {
        return 'Unknown';
    }


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
            return '⚪ Unknown';
    }
}


// ============================================================
// GMAIL COMPOSE OBSERVER
// ============================================================

function startObserver() {

    // Detect compose windows immediately.
    scanForComposeWindows();


    // Setup cleanup listeners.
    setupSendButtonCleanup();
    setupNavigationCleanup();


    // Gmail dynamically changes the DOM.
    const observer = new MutationObserver(() => {

        scanForComposeWindows();

    });


    observer.observe(document.body, {
        childList: true,
        subtree: true
    });


    // Extra scan because Gmail sometimes changes
    // compose elements without predictable mutations.
    setInterval(() => {

        scanForComposeWindows();

    }, 1500);
}


// ============================================================
// START EXTENSION
// ============================================================

if (document.readyState === 'loading') {

    document.addEventListener(
        'DOMContentLoaded',
        startObserver,
        { once: true }
    );

} else {

    startObserver();

}