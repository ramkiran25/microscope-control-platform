import requests


class MicroscopeApiError(Exception):
    """Raised with the backend's actual message, not a generic HTTP error string."""


class MicroscopeApiClient:

    def __init__(self, base_url="http://localhost:8080"):
        self.base_url = base_url.rstrip("/")

    def _post(self, path, timeout):
        try:
            response = requests.post(f"{self.base_url}{path}", timeout=timeout)
        except requests.exceptions.ConnectionError:
            raise MicroscopeApiError(
                "Could not connect to backend — is Spring Boot running on port 8080?"
            )
        except requests.exceptions.Timeout:
            raise MicroscopeApiError(f"Request to {path} timed out after {timeout}s")

        body_text = response.text

        if not response.ok:
            raise MicroscopeApiError(body_text or f"HTTP {response.status_code}")

        return body_text

    def run_z_stack(self):
        return self._post("/acquisition/z-stack", timeout=30)

    def reset(self):
        return self._post("/acquisition/reset", timeout=10)

    def start_continuous(self):
        return self._post("/acquisition/continuous/start", timeout=10)

    def stop_continuous(self):
        return self._post("/acquisition/continuous/stop", timeout=10)