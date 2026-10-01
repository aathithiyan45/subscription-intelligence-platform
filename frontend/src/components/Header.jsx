import { useLocation, useNavigate } from "react-router-dom";
import { logoutUser } from "../services/authService";

const PAGE_META = {
    "/dashboard": {
        title: "Dashboard",
        description: "Overview of your subscriptions and spending analytics",
    },
    "/subscriptions": {
        title: "Subscriptions",
        description: "Manage and monitor your active subscription services",
    },
    "/intelligence": {
        title: "Decision Intelligence",
        description: "Threshold rules, optimization recommendations, and renewal risk analysis",
    },
};

function Header() {
    const location = useLocation();
    const navigate = useNavigate();

    const email = localStorage.getItem("email") || "User";
    const currentMeta = PAGE_META[location.pathname] || {
        title: "Subscription Manager",
        description: "Subscription Intelligence Platform",
    };

    const handleLogout = () => {
        logoutUser();
        navigate("/", { replace: true });
    };

    return (
        <header className="header">
            <div className="header-page-info">
                <h1 className="header-title">{currentMeta.title}</h1>
                <p className="header-description">{currentMeta.description}</p>
            </div>

            <div className="header-user-actions">
                <div className="user-badge">
                    <span className="user-avatar-placeholder">
                        {email.charAt(0).toUpperCase()}
                    </span>
                    <span className="user-email">{email}</span>
                </div>

                <button
                    onClick={handleLogout}
                    className="logout-button"
                    title="Sign out of your account"
                >
                    Logout
                </button>
            </div>
        </header>
    );
}

export default Header;
