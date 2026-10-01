import api from "./api";

export const getSubscriptions = async (userId, params = {}) => {
    const { page = 0, size = 10, status, category } = params;
    const queryParams = { page, size };
    if (status) queryParams.status = status;
    if (category) queryParams.category = category;

    const response = await api.get(`/users/${userId}/subscriptions`, {
        params: queryParams,
    });
    return response.data;
};

export const getSubscription = async (userId, subscriptionId) => {
    const response = await api.get(`/users/${userId}/subscriptions/${subscriptionId}`);
    return response.data;
};

export const createSubscription = async (userId, subscriptionData) => {
    const response = await api.post(`/users/${userId}/subscriptions`, subscriptionData);
    return response.data;
};

export const updateSubscription = async (userId, subscriptionId, subscriptionData) => {
    const response = await api.put(`/users/${userId}/subscriptions/${subscriptionId}`, subscriptionData);
    return response.data;
};

export const deleteSubscription = async (userId, subscriptionId) => {
    const response = await api.delete(`/users/${userId}/subscriptions/${subscriptionId}`);
    return response.data;
};

export const markSubscriptionUsed = async (userId, subscriptionId) => {
    const response = await api.patch(`/users/${userId}/subscriptions/${subscriptionId}/mark-used`);
    return response.data;
};
