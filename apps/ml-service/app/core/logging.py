import logging
import sys


def setup_logging(log_level: str = "INFO") -> None:
    """Configure structured logging for the FastAPI application."""
    numeric_level = getattr(logging, log_level.upper(), logging.INFO)
    logging.basicConfig(
        level=numeric_level,
        format="%(asctime)s | %(levelname)-8s | %(name)s | %(message)s",
        handlers=[logging.StreamHandler(sys.stdout)],
        force=True,
    )


def get_logger(name: str) -> logging.Logger:
    """Get a logger instance with the standard service name prefix."""
    return logging.getLogger(name)
