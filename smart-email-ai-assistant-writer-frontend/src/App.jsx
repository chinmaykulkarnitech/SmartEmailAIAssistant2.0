import { useState } from "react";
import axios from "axios";
import {
  Container,
  Typography,
  Box,
  TextField,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Button,
  CircularProgress
} from "@mui/material";

import "./App.css";

function App() {
  const [emailContent, setEmailContent] = useState("");
  const [apiKey, setApiKey] = useState("");
  const [tone, setTone] = useState("professional");
  const [loading, setLoading] = useState(false);
  const [generatedReply, setGeneratedReply] = useState("");
  const [error, setError] = useState("");

  const handleSubmit = async () => {
    if (!emailContent.trim()) {
      setError("Please enter the original email.");
      return;
    }

    if (!apiKey.trim()) {
      setError("Please enter your Gemini API key.");
      return;
    }

    setLoading(true);
    setError("");
    setGeneratedReply("");

    try {
      console.log("API KEY PRESENT:", !!apiKey);
      console.log("API KEY LENGTH:", apiKey.length);

      const response = await axios.post(
        "http://localhost:9090/api/email/generate",
        {
          emailContent: emailContent.trim(),
          tone,
          apiKey: apiKey.trim()
        }
      );

      setGeneratedReply(
        typeof response.data === "string"
          ? response.data
          : JSON.stringify(response.data)
      );
    } catch (err) {
      console.error("Generate reply error:", err);

      const status = err?.response?.status;
      const serverMessage = err?.response?.data;

      if (status === 503) {
        setError(
          "Gemini is temporarily unavailable. Please try again in a few seconds."
        );
      } else if (status === 401 || status === 403) {
        setError(
          "The Gemini API key is invalid or unauthorized."
        );
      } else if (status === 429) {
        setError(
          "Gemini API rate limit reached. Please try again later."
        );
      } else if (status === 500) {
        setError(
          "Backend error. The request reached the server, but the server encountered an internal error."
        );
      } else if (serverMessage) {
        setError(String(serverMessage));
      } else {
        setError("Request failed. Please check the backend.");
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <Container maxWidth="md" sx={{ py: 4 }}>
      <Typography
        variant="h3"
        fontWeight="bold"
        component="h1"
        gutterBottom
      >
        Smart Email AI Assistant
      </Typography>

      <Box sx={{ mt: 3 }}>
        <TextField
          fullWidth
          type="password"
          label="Gemini API Key"
          placeholder="Enter your Gemini API key"
          value={apiKey}
          onChange={(e) => setApiKey(e.target.value)}
          sx={{ mb: 2 }}
        />

        <TextField
          fullWidth
          multiline
          rows={6}
          variant="outlined"
          label="Original Email Content"
          placeholder="Paste the email you want to reply to..."
          value={emailContent}
          onChange={(e) => setEmailContent(e.target.value)}
          sx={{ mb: 2 }}
        />

        <FormControl fullWidth>
          <InputLabel>Email Tone</InputLabel>

          <Select
            value={tone}
            label="Email Tone"
            onChange={(e) => setTone(e.target.value)}
          >
            <MenuItem value="professional">Professional</MenuItem>
            <MenuItem value="friendly">Friendly</MenuItem>
            <MenuItem value="formal">Formal</MenuItem>
            <MenuItem value="polite">Polite</MenuItem>
            <MenuItem value="confident">Confident</MenuItem>
            <MenuItem value="apologetic">Apologetic</MenuItem>
            <MenuItem value="appreciative">Appreciative</MenuItem>
            <MenuItem value="empathetic">Empathetic</MenuItem>
            <MenuItem value="concise">Concise</MenuItem>
          </Select>
        </FormControl>

        <Button
          variant="contained"
          sx={{ mt: 2 }}
          onClick={handleSubmit}
          disabled={
            !emailContent.trim() ||
            !apiKey.trim() ||
            loading
          }
        >
          {loading ? (
            <CircularProgress size={24} color="inherit" />
          ) : (
            "Generate Reply"
          )}
        </Button>

        {error && (
          <Typography color="error" sx={{ mt: 2 }}>
            {error}
          </Typography>
        )}
      </Box>

      <Box sx={{ mt: 3 }}>
        <TextField
          fullWidth
          multiline
          rows={8}
          variant="outlined"
          label="Generated Reply"
          value={generatedReply}
          slotProps={{
            input: {
              readOnly: true
            }
          }}
        />

        <Button
          sx={{ mt: 2 }}
          variant="outlined"
          disabled={!generatedReply}
          onClick={() =>
            navigator.clipboard.writeText(generatedReply)
          }
        >
          Copy AI Generated Reply
        </Button>
      </Box>
    </Container>
  );
}

export default App;
