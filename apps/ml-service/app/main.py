from contextlib import asynccontextmanager
from typing import AsyncGenerator
from fastapi import FastAPI, Request, status
from fastapi.exceptions import RequestValidationError
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse

from app.api.routes.health import router as health_router
from app.api.routes.prediction import router as prediction_router
from app.config import get_settings
from app.core.errors import (
    FeatureTransformationException,
    MLServiceException,
    ModelNotAvailableException,
)
from app.core.logging import get_logger, setup_logging
from app.schemas.error import ErrorResponse

logger = get_logger("ml_service.main")


@asynccontextmanager
async def lifespan(app: FastAPI) -> AsyncGenerator[None, None]:
    """Application startup and shutdown lifespan events."""
    settings = get_settings()
    setup_logging(settings.LOG_LEVEL)
    logger.info(
        "Starting %s v%s (environment: %s, port: %d)",
        settings.APP_NAME,
        settings.APP_VERSION,
        settings.APP_ENV,
        settings.APP_PORT,
    )
    yield
    logger.info("Shutting down %s", settings.APP_NAME)


def create_app() -> FastAPI:
    """FastAPI application factory."""
    settings = get_settings()

    app = FastAPI(
        title="Intelligent Supply Chain Disruption Prediction ML Service",
        description=(
            "FastAPI ML Service providing ML feature engineering, model inference, "
            "and disruption probability predictions for the Intelligent Supply Chain Disruption platform."
        ),
        version=settings.APP_VERSION,
        docs_url="/docs",
        redoc_url="/redoc",
        openapi_url="/openapi.json",
        lifespan=lifespan,
    )

    # CORS configuration
    app.add_middleware(
        CORSMiddleware,
        allow_origins=["*"],
        allow_credentials=True,
        allow_methods=["*"],
        allow_headers=["*"],
    )

    # Global Exception Handlers
    @app.exception_handler(RequestValidationError)
    async def validation_exception_handler(request: Request, exc: RequestValidationError) -> JSONResponse:
        logger.warning("Validation error on path %s: %s", request.url.path, exc.errors())
        error_payload = ErrorResponse(
            status=422,
            error="Validation Error",
            message="Request body failed validation constraints.",
            path=request.url.path,
            details=exc.errors(),
        )
        return JSONResponse(
            status_code=422,
            content=error_payload.model_dump(mode="json"),
        )

    @app.exception_handler(ModelNotAvailableException)
    async def model_not_available_handler(request: Request, exc: ModelNotAvailableException) -> JSONResponse:
        logger.info("Model not available request on path %s: %s", request.url.path, exc.message)
        error_payload = ErrorResponse(
            status=exc.status_code,
            error="Model Not Available",
            message=exc.message,
            path=request.url.path,
        )
        return JSONResponse(
            status_code=exc.status_code,
            content=error_payload.model_dump(mode="json"),
        )

    @app.exception_handler(FeatureTransformationException)
    async def feature_transformation_handler(request: Request, exc: FeatureTransformationException) -> JSONResponse:
        logger.warning("Feature transformation error on path %s: %s", request.url.path, exc.message)
        error_payload = ErrorResponse(
            status=exc.status_code,
            error="Feature Transformation Error",
            message=exc.message,
            path=request.url.path,
        )
        return JSONResponse(
            status_code=exc.status_code,
            content=error_payload.model_dump(mode="json"),
        )

    @app.exception_handler(MLServiceException)
    async def ml_service_exception_handler(request: Request, exc: MLServiceException) -> JSONResponse:
        logger.error("ML service exception on path %s: %s", request.url.path, exc.message)
        error_payload = ErrorResponse(
            status=exc.status_code,
            error="ML Service Error",
            message=exc.message,
            path=request.url.path,
        )
        return JSONResponse(
            status_code=exc.status_code,
            content=error_payload.model_dump(mode="json"),
        )

    @app.exception_handler(Exception)
    async def generic_exception_handler(request: Request, exc: Exception) -> JSONResponse:
        logger.error("Unhandled exception on path %s", request.url.path, exc_info=exc)
        error_payload = ErrorResponse(
            status=status.HTTP_500_INTERNAL_SERVER_ERROR,
            error="Internal Server Error",
            message="An unexpected internal server error occurred.",
            path=request.url.path,
        )
        return JSONResponse(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            content=error_payload.model_dump(mode="json"),
        )

    # Register Routers
    app.include_router(health_router, prefix="/api")
    app.include_router(prediction_router, prefix="/api")

    return app


app = create_app()
