package com.sih.supplychain.client.ml;

import com.fasterxml.jackson.annotation.JsonProperty;

public record FastApiPredictionRequest(
        @JsonProperty("hist_otdr_90d") Double histOtdr90d,
        @JsonProperty("hist_avg_delay_90d") Double histAvgDelay90d,
        @JsonProperty("hist_fulfillment_rate_90d") Double histFulfillmentRate90d,
        @JsonProperty("hist_disruptions_90d") Integer histDisruptions90d,
        @JsonProperty("supplier_lead_time_contract") Integer supplierLeadTimeContract,
        @JsonProperty("material_criticality") String materialCriticality,
        @JsonProperty("order_volume_ratio") Double orderVolumeRatio,
        @JsonProperty("inventory_coverage_days") Double inventoryCoverageDays,
        @JsonProperty("po_line_value") Double poLineValue,
        @JsonProperty("supplier_country") String supplierCountry
) {
}
