function SummaryCard({ title, value, icon, subtitle, badgeText, badgeType = "neutral" }) {
    return (
        <div className="summary-card">
            <div className="summary-card-header">
                <span className="summary-card-title">{title}</span>
                {icon && <div className="summary-card-icon">{icon}</div>}
            </div>
            <div className="summary-card-body">
                <div className="summary-card-value">{value}</div>
                {badgeText && (
                    <span className={`summary-card-badge badge-${badgeType}`}>
                        {badgeText}
                    </span>
                )}
            </div>
            {subtitle && <div className="summary-card-subtitle">{subtitle}</div>}
        </div>
    );
}

export default SummaryCard;
