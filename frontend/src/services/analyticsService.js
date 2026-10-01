import api from "./api";

export const getSpendingAnalytics = async (userId, period = "monthly") => {
    const response = await api.get(`/users/${userId}/analytics/spending`, {
        params: { period },
    });
    return response.data;
};

export const getRenewalRisks = async (userId) => {
    const response = await api.get(`/users/${userId}/analytics/renewal-risk`);
    return response.data;
};

export const getEfficiencyScores = async (userId) => {
    const response = await api.get(`/users/${userId}/analytics/efficiency-scores`);
    return response.data;
};

export const getOptimizationOpportunities = async (userId) => {
    const response = await api.get(`/users/${userId}/analytics/optimization-opportunities`);
    return response.data;
};

export const getOverlaps = async (userId) => {
    const response = await api.get(`/users/${userId}/analytics/overlaps`);
    return response.data;
};
