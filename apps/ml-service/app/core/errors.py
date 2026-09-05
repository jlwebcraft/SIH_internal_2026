class MLServiceException(Exception):
    """Base exception for ML service errors."""

    def __init__(self, message: str, status_code: int = 500) -> None:
        super().__init__(message)
        self.message = message
        self.status_code = status_code


class ModelNotAvailableException(MLServiceException):
    """Exception raised when an ML model artifact is not loaded or available."""

    def __init__(
        self,
        message: str = "Disruption prediction ML model is not yet trained or available (Phase 7C foundation).",
    ) -> None:
        super().__init__(message=message, status_code=503)


class FeatureTransformationException(MLServiceException):
    """Exception raised when feature transformation fails on invalid or inconsistent inputs."""

    def __init__(self, message: str) -> None:
        super().__init__(message=message, status_code=422)
