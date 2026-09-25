import { useEffect, useState } from "react";
import api from "../services/api";

function Dashboard() {
    const [subscriptions, setSubscriptions] = useState(null);
    const [error, setError] = useState("");

    useEffect(() => {
        const fetchSubscriptions = async () => {
            try {
                const userId = localStorage.getItem("userId");

                const response = await api.get(
                    `/users/${userId}/subscriptions`
                );

                setSubscriptions(response.data);
            } catch (error) {
                console.error(error);
                setError("Failed to load subscriptions");
            }
        };

        fetchSubscriptions();
    }, []);

    const logout = () => {
        localStorage.clear();
        window.location.href = "/";
    };

    return (
        <div>
            <h1>Subscription Manager</h1>

            <p>
                Welcome, {localStorage.getItem("email")}
            </p>

            <button onClick={logout}>
                Logout
            </button>

            <hr />

            <h2>My Subscriptions</h2>

            {error && <p>{error}</p>}

            {subscriptions && (
                <pre>
                    {JSON.stringify(subscriptions, null, 2)}
                </pre>
            )}
        </div>
    );
}

export default Dashboard;