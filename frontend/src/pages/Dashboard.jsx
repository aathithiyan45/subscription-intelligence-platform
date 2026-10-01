import { useEffect, useState, useCallback } from "react";
import { getSpendingAnalytics, getRenewalRisks } from "../services/analyticsService";
import SummaryCard from "../components/SummaryCard";
import Loading from "../components/Loading";
import ErrorMessage from "../components/ErrorMessage";

function Dashboard() {
    const [analytics, setAnalytics] = useState(null);
    const [renewalRisks, setRenewalRisks] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    const fetchDashboardData = useCallback(async () => {
        setLoading(true);
        setError(null);

        try {
            const userId = localStorage.getItem("userId");
            if (!userId) {
                throw new Error("Authenticated user ID not found in session state.");
            }

            const [analyticsData, risksData] = await Promise.all([
                getSpendingAnalytics(userId),
                getRenewalRisks(userId).catch(() => []),
            ]);

            setAnalytics(analyticsData);
            setRenewalRisks(risksData || []);
        } catch (err) {
            console.error("Dashboard fetch error:", err);
            setError(err.response?.data?.message || err.message || "Failed to load dashboard metrics");
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => {
        fetchDashboardData();
    }, [fetchDashboardData]);

    if (loading) {
        return <Loading message="Loading your subscription intelligence overview..." />;
    }

    if (error) {
        return (
            <ErrorMessage
                title="Dashboard Load Failed"
                message={error}
                onRetry={fetchDashboardData}
            />
        );
    }

    const monthlySpend = analytics?.totalMonthlySpend ?? 0;
    const yearlySpend = monthlySpend * 12;
    const totalActive = analytics?.totalActiveSubscriptions ?? 0;
    const categoryBreakdown = analytics?.byCategory ?? [];
    const highRiskCount = renewalRisks.filter((r) => r.riskTier === "HIGH").length;

    return (
        <div className="dashboard-content">
            {/* KPI Summary Cards Grid */}
            <div className="summary-grid">
                <SummaryCard
                    title="Total Subscriptions"
                    value={totalActive}
                    icon="💳"
                    subtitle="Tracked in account"
                    badgeText="Active"
                    badgeType="success"
                />

                <SummaryCard
                    title="Monthly Spending"
                    value={`$${Number(monthlySpend).toFixed(2)}`}
                    icon="💵"
                    subtitle="Current monthly commitment"
                    badgeText="Monthly"
                    badgeType="primary"
                />

                <SummaryCard
                    title="Yearly Spending"
                    value={`$${Number(yearlySpend).toFixed(2)}`}
                    icon="📈"
                    subtitle="Annual projected expenditure"
                    badgeText="Annual Proj."
                    badgeType="neutral"
                />

                <SummaryCard
                    title="Upcoming Renewals / Risks"
                    value={renewalRisks.length}
                    icon="⚠️"
                    subtitle={`${highRiskCount} high risk hike alerts`}
                    badgeText={highRiskCount > 0 ? "Attention Needed" : "Normal Tier"}
                    badgeType={highRiskCount > 0 ? "danger" : "success"}
                />

                <SummaryCard
                    title="Active Subscriptions"
                    value={totalActive}
                    icon="⚡"
                    subtitle="In good status"
                    badgeText="100% Monitored"
                    badgeType="info"
                />
            </div>

            {/* Renewal Risk Warnings Section */}
            {renewalRisks.length > 0 && (
                <div className="dashboard-section">
                    <div className="section-header">
                        <h3>Renewal Price Hike Risks ({renewalRisks.length})</h3>
                        <span className="section-subtitle">Identified by Backend Decision Engine</span>
                    </div>
                    <div className="risks-list">
                        {renewalRisks.map((risk) => (
                            <div key={risk.subscriptionId || risk.subscriptionName} className={`risk-item-card risk-tier-${risk.riskTier?.toLowerCase()}`}>
                                <div className="risk-header">
                                    <span className="risk-name">{risk.subscriptionName}</span>
                                    <span className={`risk-badge badge-${risk.riskTier === "HIGH" ? "danger" : risk.riskTier === "MEDIUM" ? "warning" : "info"}`}>
                                        {risk.riskTier} RISK
                                    </span>
                                </div>
                                <div className="risk-details">
                                    <span>Current: <strong>${Number(risk.currentPrice).toFixed(2)}</strong></span>
                                    {risk.previousPrice && <span>Previous: ${Number(risk.previousPrice).toFixed(2)}</span>}
                                    {risk.totalIncreasePercentage > 0 && (
                                        <span className="risk-hike">+{risk.totalIncreasePercentage.toFixed(1)}% Hike</span>
                                    )}
                                </div>
                                {risk.recommendation && (
                                    <p className="risk-recommendation">💡 {risk.recommendation}</p>
                                )}
                            </div>
                        ))}
                    </div>
                </div>
            )}

            {/* Category Breakdown Section */}
            <div className="dashboard-section">
                <div className="section-header">
                    <h3>Spending by Category</h3>
                    <span className="section-subtitle">Real breakdown from backend analytics</span>
                </div>

                {categoryBreakdown.length === 0 ? (
                    <div className="empty-state-box">
                        <p>No category spending data available yet.</p>
                    </div>
                ) : (
                    <div className="category-grid">
                        {categoryBreakdown.map((item) => (
                            <div key={item.category} className="category-card">
                                <div className="category-card-header">
                                    <span className="category-name">{item.category}</span>
                                    <span className="category-count">{item.subscriptionCount} subs</span>
                                </div>
                                <div className="category-card-body">
                                    <span className="category-spend">${Number(item.totalSpend).toFixed(2)}/mo</span>
                                    {item.percentageOfTotal != null && (
                                        <span className="category-percentage">{(item.percentageOfTotal * 100).toFixed(0)}% of total</span>
                                    )}
                                </div>
                                {item.percentageOfTotal != null && (
                                    <div className="progress-bar-bg">
                                        <div
                                            className="progress-bar-fill"
                                            style={{ width: `${Math.min(100, Math.max(0, item.percentageOfTotal * 100))}%` }}
                                        ></div>
                                    </div>
                                )}
                            </div>
                        ))}
                    </div>
                )}
            </div>
        </div>
    );
}

export default Dashboard;