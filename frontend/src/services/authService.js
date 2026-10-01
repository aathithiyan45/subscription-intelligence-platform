import api from "./api";

export const loginUser = async (email, password) => {
    const response = await api.post("/auth/login", {
        email,
        password,
    });

    return response.data;
};

export const logoutUser = () => {
    localStorage.removeItem("token");
    localStorage.removeItem("userId");
    localStorage.removeItem("email");
    localStorage.removeItem("role");
};

export const getStoredUser = () => {
    return {
        token: localStorage.getItem("token"),
        userId: localStorage.getItem("userId"),
        email: localStorage.getItem("email"),
        role: localStorage.getItem("role"),
    };
};

export const isAuthenticated = () => {
    return !!localStorage.getItem("token");
};