from abc import ABC, abstractmethod
from pathlib import Path
from typing import Optional
import numpy as np

from app.core.errors import ModelNotAvailableException


class BaseDisruptionModel(ABC):
    """Abstract base class defining the contract for disruption prediction models."""

    @abstractmethod
    def is_available(self) -> bool:
        """Check if a trained model artifact is loaded and ready for inference."""
        pass

    @abstractmethod
    def load(self, model_path: Optional[Path] = None) -> None:
        """Load the model artifact from disk or storage."""
        pass

    @abstractmethod
    def predict(self, features: np.ndarray) -> np.ndarray:
        """Generate binary class predictions (0 = On-Time, 1 = Disrupted)."""
        pass

    @abstractmethod
    def predict_proba(self, features: np.ndarray) -> np.ndarray:
        """Generate predicted probabilities of disruption for given feature vectors."""
        pass

    @property
    @abstractmethod
    def model_version(self) -> str:
        """Return the unique version identifier of the loaded model artifact."""
        pass


class UntrainedDisruptionModel(BaseDisruptionModel):
    """Default placeholder model representing an uninitialized/untrained model state."""

    def __init__(self) -> None:
        self._version = "none"

    def is_available(self) -> bool:
        return False

    def load(self, model_path: Optional[Path] = None) -> None:
        # No trained artifact exists in Phase 7C foundation
        pass

    def predict(self, features: np.ndarray) -> np.ndarray:
        raise ModelNotAvailableException("No trained ML model artifact is loaded (Phase 7C foundation).")

    def predict_proba(self, features: np.ndarray) -> np.ndarray:
        raise ModelNotAvailableException("No trained ML model artifact is loaded (Phase 7C foundation).")

    @property
    def model_version(self) -> str:
        return self._version
