import { useState, useEffect, useCallback } from "react";
import {
    getOptimizationOpportunities,
    getEfficiencyScores,
    getOverlaps,
    getRenewalRisks,
} from "../services/analyticsService";
import { markSubscriptionUsed } from "../services/subscriptionService";
import Loading from "../components/Loading";
import ErrorMessage from "../components/ErrorMessage";

function IntelligencePage() {
    const [optimizations, setOptimizations] = useState([]);
    const [efficiencyScores, setEfficiencyScores] = useState([]);
    const [overlaps, setOverlaps] = useState([]);
    const [renewalRisks, setRenewalRisks] = useState([]);
    
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [activeTab, setActiveTab] = useState("all");
    const [actionMessage, setActionMessage] = useState("");

    const fetchIntelligenceData = useCallback(async () => {
        setLoading(true);
        setError(null);

        try {
            const userId = localStorage.getItem("userId");
            if (!userId) throw new Error("Authenticated user ID not found");

            const [optData, effData, ovData, riskData] = await Promise.all([
                getOptimizationOpportunities(userId).catch(() => []),
                getEfficiencyScores(userId).catch(() => []),
                getOverlaps(userId).catch(() => []),
                getRenewalRisks(userId).catch(() => []),
            ]);

            setOptimizations(optData || []);
            setEfficiencyScores(effData || []);
            setOverlaps(ovData || []);
            setRenewalRisks(riskData || []);
        } catch (err) {
            console.error("Fetch intelligence data error:", err);
            setError(err.response?.data?.message || err.message || "Failed to load decision intelligence");
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => {
        fetchIntelligenceData();
    }, [fetchIntelligenceData]);

    const handleMarkUsed = async (subscriptionId, subName) => {
        try {
            const userId = localStorage.getItem("userId");
            await markSubscriptionUsed(userId, subscriptionId);
            setActionMessage(`Marked "${subName}" as used today! Efficiency updated.`);
            setTimeout(() => setActionMessage(""), 4000);
            fetchIntelligenceData();
        } catch (err) {
            console.error("Mark used error:", err);
            setError("Failed to update usage timestamp.");
        }
    };

    if (loading) {
        return <Loading message="Evaluating threshold decision engine rules..." />;
    }

    if (error) {
        return (
            <ErrorMessage
                title="Intelligence Engine Error"
                message={error}
                onRetry={fetchIntelligenceData}
            />
        );
    }

    // Calculate overall potential annual savings
    const totalAnnualSavings = optimizations.reduce(
        (sum, item) => sum + (Number(item.estimatedAnnualSavings) || 0),
        0
    );

    const underutilizedCount = efficiencyScores.filter(
        (e) => e.efficiencyTier === "UNDERUTILIZED" || e.efficiencyTier === "NEVER_USED"
    ).length;

    return (
        <div className="intelligence-page-container">
            {/* Banner Messages */}
            {actionMessage && (
                <div className="toast-success">
                    <span>⚡ {actionMessage}</span>
                </div>
            )}

            {/* Decision Engine KPI Highlight */}
            <div className="intelligence-hero-card">
                <div className="hero-stat-item">
                    <span className="hero-stat-label">Potential Annual Savings</span>
                    <span className="hero-stat-value text-success">${totalAnnualSavings.toFixed(2)}</span>
                    <span className="hero-stat-desc">Identified by threshold engine</span>
                </div>

                <div className="hero-stat-divider"></div>

                <div className="hero-stat-item">
                    <span className="hero-stat-label">Optimization Opportunities</span>
                    <span className="hero-stat-value">{optimizations.length}</span>
                    <span className="hero-stat-desc">Actionable savings rules</span>
                </div>

                <div className="hero-stat-divider"></div>

                <div className="hero-stat-item">
                    <span className="hero-stat-label">Underutilized Subscriptions</span>
                    <span className="hero-stat-value text-warning">{underutilizedCount}</span>
                    <span className="hero-stat-desc">Low usage flags</span>
                </div>

                <div className="hero-stat-divider"></div>

                <div className="hero-stat-item">
                    <span className="hero-stat-label">Category Overlaps</span>
                    <span className="hero-stat-value">{overlaps.length}</span>
                    <span className="hero-stat-desc">Redundant category clusters</span>
                </div>
            </div>

            {/* Tab Filter Bar */}
            <div className="intelligence-tabs">
                <button
                    className={`intel-tab ${activeTab === "all" ? "active" : ""}`}
                    onClick={() => setActiveTab("all")}
                >
                    All Intelligence ({optimizations.length + efficiencyScores.length + overlaps.length + renewalRisks.length})
                </button>
                <button
                    className={`intel-tab ${activeTab === "optimizations" ? "active" : ""}`}
                    onClick={() => setActiveTab("optimizations")}
                >
                    Savings Opportunities ({optimizations.length})
                </button>
                <button
                    className={`intel-tab ${activeTab === "efficiency" ? "active" : ""}`}
                    onClick={() => setActiveTab("efficiency")}
                >
                    Usage & Efficiency ({efficiencyScores.length})
                </button>
                <button
                    className={`intel-tab ${activeTab === "overlaps" ? "active" : ""}`}
                    onClick={() => setActiveTab("overlaps")}
                >
                    Category Overlaps ({overlaps.length})
                </button>
                <button
                    className={`intel-tab ${activeTab === "risks" ? "active" : ""}`}
                    onClick={() => setActiveTab("risks")}
                >
                    Price Hike Risks ({renewalRisks.length})
                </button>
            </div>

            {/* 1. OPTIMIZATION OPPORTUNITIES */}
            {(activeTab === "all" || activeTab === "optimizations") && (
                <div className="dashboard-section">
                    <div className="section-header">
                        <h3>💡 Optimization Opportunities</h3>
                        <span className="section-subtitle">Rule-based recommendations to lower subscription spend</span>
                    </div>

                    {optimizations.length === 0 ? (
                        <div className="empty-state-box">
                            <p>No optimization opportunities flagged. Your subscriptions are performing well!</p>
                        </div>
                    ) : (
                        <div className="intelligence-grid">
                            {optimizations.map((item) => (
                                <div key={item.subscriptionId || item.subscriptionName} className="intel-card border-left-primary">
                                    <div className="intel-card-header">
                                        <div>
                                            <h4 className="intel-title">{item.subscriptionName}</h4>
                                            <span className="category-pill" style={{ marginTop: "0.25rem", display: "inline-block" }}>
                                                {item.category}
                                            </span>
                                        </div>
                                        <span className={`summary-card-badge badge-${item.opportunityTier === "HIGH" ? "danger" : item.opportunityTier === "MEDIUM" ? "warning" : "info"}`}>
                                            {item.opportunityTier} TIER
                                        </span>
                                    </div>

                                    <div className="intel-metrics-row">
                                        <div className="intel-metric">
                                            <span className="metric-label">Monthly Cost</span>
                                            <span className="metric-val">${Number(item.monthlyPrice).toFixed(2)}</span>
                                        </div>
                                        <div className="intel-metric">
                                            <span className="metric-label">Est. Annual Savings</span>
                                            <span className="metric-val text-success">${Number(item.estimatedAnnualSavings).toFixed(2)}/yr</span>
                                        </div>
                                        <div className="intel-metric">
                                            <span className="metric-label">Score</span>
                                            <span className="metric-val">{(item.score * 100).toFixed(0)}%</span>
                                        </div>
                                    </div>

                                    {item.reasons && item.reasons.length > 0 && (
                                        <div className="intel-reasons">
                                            <span className="reasons-title">Decision Engine Reasons:</span>
                                            <ul>
                                                {item.reasons.map((reason, idx) => (
                                                    <li key={idx}>{reason}</li>
                                                ))}
                                            </ul>
                                        </div>
                                    )}

                                    {item.suggestedAction && (
                                        <div className="suggested-action-box">
                                            🎯 <strong>Suggested Action:</strong> {item.suggestedAction}
                                        </div>
                                    )}
                                </div>
                            ))}
                        </div>
                    )}
                </div>
            )}

            {/* 2. USAGE & EFFICIENCY SCORES */}
            {(activeTab === "all" || activeTab === "efficiency") && (
                <div className="dashboard-section">
                    <div className="section-header">
                        <h3>📊 Usage Efficiency & Inactivity Analysis</h3>
                        <span className="section-subtitle">Evaluated by days since last used vs. billing threshold rules</span>
                    </div>

                    {efficiencyScores.length === 0 ? (
                        <div className="empty-state-box">
                            <p>No usage efficiency scores calculated yet.</p>
                        </div>
                    ) : (
                        <div className="intelligence-grid">
                            {efficiencyScores.map((eff) => (
                                <div
                                    key={eff.subscriptionId || eff.subscriptionName}
                                    className={`intel-card ${
                                        eff.efficiencyTier === "NEVER_USED"
                                            ? "border-left-danger"
                                            : eff.efficiencyTier === "UNDERUTILIZED"
                                            ? "border-left-warning"
                                            : "border-left-success"
                                    }`}
                                >
                                    <div className="intel-card-header">
                                        <h4 className="intel-title">{eff.subscriptionName}</h4>
                                        <span className={`summary-card-badge badge-${
                                            eff.efficiencyTier === "OPTIMAL" ? "success" : eff.efficiencyTier === "UNDERUTILIZED" ? "warning" : "danger"
                                        }`}>
                                            {eff.efficiencyTier}
                                        </span>
                                    </div>

                                    <div className="intel-metrics-row">
                                        <div className="intel-metric">
                                            <span className="metric-label">Efficiency Score</span>
                                            <span className="metric-val">{(eff.efficiencyScore * 100).toFixed(0)} / 100</span>
                                        </div>
                                        <div className="intel-metric">
                                            <span className="metric-label">Days Inactive</span>
                                            <span className="metric-val">{eff.daysSinceLastUsed != null ? `${eff.daysSinceLastUsed} days` : "Never used"}</span>
                                        </div>
                                        <div className="intel-metric">
                                            <span className="metric-label">Monthly Price</span>
                                            <span className="metric-val">${Number(eff.monthlyPrice).toFixed(2)}</span>
                                        </div>
                                    </div>

                                    {/* Efficiency Progress Bar */}
                                    <div className="progress-bar-bg" style={{ margin: "0.5rem 0" }}>
                                        <div
                                            className="progress-bar-fill"
                                            style={{
                                                width: `${Math.min(100, Math.max(0, eff.efficiencyScore * 100))}%`,
                                                backgroundColor: eff.efficiencyTier === "OPTIMAL" ? "var(--success)" : eff.efficiencyTier === "UNDERUTILIZED" ? "var(--warning)" : "var(--danger)",
                                            }}
                                        ></div>
                                    </div>

                                    {eff.recommendation && (
                                        <p className="intel-recommendation">💡 {eff.recommendation}</p>
                                    )}

                                    {eff.subscriptionId && (
                                        <button
                                            onClick={() => handleMarkUsed(eff.subscriptionId, eff.subscriptionName)}
                                            className="btn-secondary btn-sm"
                                            style={{ marginTop: "0.5rem" }}
                                        >
                                            ⚡ I Used This Today
                                        </button>
                                    )}
                                </div>
                            ))}
                        </div>
                    )}
                </div>
            )}

            {/* 3. CATEGORY OVERLAPS */}
            {(activeTab === "all" || activeTab === "overlaps") && (
                <div className="dashboard-section">
                    <div className="section-header">
                        <h3>🔄 Category Overlaps & Redundancies</h3>
                        <span className="section-subtitle">Detects multiple active subscriptions competing in the same category</span>
                    </div>

                    {overlaps.length === 0 ? (
                        <div className="empty-state-box">
                            <p>No redundant category overlaps detected.</p>
                        </div>
                    ) : (
                        <div className="intelligence-grid">
                            {overlaps.map((ov, idx) => (
                                <div key={idx} className="intel-card border-left-warning">
                                    <div className="intel-card-header">
                                        <h4 className="intel-title">{ov.category} Category Overlap</h4>
                                        <span className="summary-card-badge badge-warning">
                                            {ov.activeSubscriptionsCount} ACTIVE SUBS
                                        </span>
                                    </div>

                                    <div className="intel-metrics-row">
                                        <div className="intel-metric">
                                            <span className="metric-label">Total Category Spend</span>
                                            <span className="metric-val">${Number(ov.totalMonthlySpendInCategory).toFixed(2)}/mo</span>
                                        </div>
                                        <div className="intel-metric">
                                            <span className="metric-label">Potential Monthly Savings</span>
                                            <span className="metric-val text-success">${Number(ov.potentialMonthlySavings).toFixed(2)}/mo</span>
                                        </div>
                                    </div>

                                    {ov.subscriptions && ov.subscriptions.length > 0 && (
                                        <div className="overlap-subs-list">
                                            <span className="reasons-title">Overlapping Subscriptions:</span>
                                            <ul>
                                                {ov.subscriptions.map((sub) => (
                                                    <li key={sub.id}>
                                                        <strong>{sub.name}</strong> — ${Number(sub.price).toFixed(2)}/{sub.billingCycle?.toLowerCase()} ({sub.status})
                                                    </li>
                                                ))}
                                            </ul>
                                        </div>
                                    )}
                                </div>
                            ))}
                        </div>
                    )}
                </div>
            )}

            {/* 4. RENEWAL PRICE HIKE RISKS */}
            {(activeTab === "all" || activeTab === "risks") && (
                <div className="dashboard-section">
                    <div className="section-header">
                        <h3>⚠️ Renewal Price Hike Risks</h3>
                        <span className="section-subtitle">Tracks price inflation across renewal cycles</span>
                    </div>

                    {renewalRisks.length === 0 ? (
                        <div className="empty-state-box">
                            <p>No renewal price hike risks flagged.</p>
                        </div>
                    ) : (
                        <div className="intelligence-grid">
                            {renewalRisks.map((risk) => (
                                <div
                                    key={risk.subscriptionId || risk.subscriptionName}
                                    className={`intel-card ${
                                        risk.riskTier === "HIGH" ? "border-left-danger" : risk.riskTier === "MEDIUM" ? "border-left-warning" : "border-left-info"
                                    }`}
                                >
                                    <div className="intel-card-header">
                                        <h4 className="intel-title">{risk.subscriptionName}</h4>
                                        <span className={`summary-card-badge badge-${risk.riskTier === "HIGH" ? "danger" : risk.riskTier === "MEDIUM" ? "warning" : "info"}`}>
                                            {risk.riskTier} RISK
                                        </span>
                                    </div>

                                    <div className="intel-metrics-row">
                                        <div className="intel-metric">
                                            <span className="metric-label">Current Price</span>
                                            <span className="metric-val">${Number(risk.currentPrice).toFixed(2)}</span>
                                        </div>
                                        <div className="intel-metric">
                                            <span className="metric-label">Price Hikes</span>
                                            <span className="metric-val">{risk.priceIncreaseCount} increases</span>
                                        </div>
                                        <div className="intel-metric">
                                            <span className="metric-label">Total Hike %</span>
                                            <span className="metric-val text-danger">+{risk.totalIncreasePercentage.toFixed(1)}%</span>
                                        </div>
                                    </div>

                                    {risk.recommendation && (
                                        <p className="intel-recommendation">💡 {risk.recommendation}</p>
                                    )}
                                </div>
                            ))}
                        </div>
                    )}
                </div>
            )}
        </div>
    );
}

export default IntelligencePage;
