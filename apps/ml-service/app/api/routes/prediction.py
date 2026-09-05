from fastapi import APIRouter, Depends

from app.schemas.prediction import DisruptionPredictionRequest, DisruptionPredictionResponse
from app.services.prediction_service import PredictionService

router = APIRouter(tags=["Inference & Prediction"])


def get_prediction_service() -> PredictionService:
    return PredictionService()


@router.post(
    "/predict/disruption",
    response_model=DisruptionPredictionResponse,
    status_code=200,
    summary="Predict supply chain disruption",
    description=(
        "Predicts disruption probability for an upcoming procurement transaction using validated features. "
        "In Phase 7C (foundation), returns HTTP 503 as no model artifact is trained or loaded."
    ),
)
def predict_disruption(
    request: DisruptionPredictionRequest,
    service: PredictionService = Depends(get_prediction_service),
) -> DisruptionPredictionResponse:
    return service.predict(request)
