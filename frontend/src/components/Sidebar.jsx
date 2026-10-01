import { NavLink } from "react-router-dom";

function Sidebar() {
    return (
        <aside className="sidebar">
            <div className="sidebar-brand">
                <div className="brand-icon">⚡</div>
                <div className="brand-text">
                    <span className="brand-name">SubIntel</span>
                    <span className="brand-tag">Platform</span>
                </div>
            </div>

            <nav className="sidebar-nav">
                <div className="nav-section-label">MENU</div>
                <NavLink
                    to="/dashboard"
                    className={({ isActive }) =>
                        `nav-link ${isActive ? "active" : ""}`
                    }
                >
                    <span className="nav-icon">📊</span>
                    <span className="nav-text">Dashboard</span>
                </NavLink>

                <NavLink
                    to="/subscriptions"
                    className={({ isActive }) =>
                        `nav-link ${isActive ? "active" : ""}`
                    }
                >
                    <span className="nav-icon">💳</span>
                    <span className="nav-text">Subscriptions</span>
                </NavLink>

                <NavLink
                    to="/intelligence"
                    className={({ isActive }) =>
                        `nav-link ${isActive ? "active" : ""}`
                    }
                >
                    <span className="nav-icon">💡</span>
                    <span className="nav-text">Intelligence</span>
                </NavLink>
            </nav>

            <div className="sidebar-footer">
                <div className="platform-version">v1.0.0 • Rule Engine</div>
            </div>
        </aside>
    );
}

export default Sidebar;
