const STATUS_BADGE_MAP = {
    ACTIVE: "success",
    PAUSED: "warning",
    CANCELLED: "danger",
    EXPIRED: "neutral",
};

function formatDate(dateString) {
    if (!dateString) return "Never";
    const date = new Date(dateString);
    if (isNaN(date.getTime())) return "N/A";
    return date.toLocaleDateString(undefined, {
        year: "numeric",
        month: "short",
        day: "numeric",
    });
}

function SubscriptionTable({
    subscriptions,
    pageInfo,
    onPageChange,
    onEdit,
    onDelete,
    onMarkUsed,
    statusFilter,
    onStatusFilterChange,
    categoryFilter,
    onCategoryFilterChange,
    onOpenAddModal,
}) {
    const CATEGORIES = ["ALL", "ENTERTAINMENT", "SOFTWARE", "UTILITIES", "FITNESS", "WORK", "EDUCATION", "OTHER"];
    const STATUSES = ["ALL", "ACTIVE", "PAUSED", "CANCELLED", "EXPIRED"];

    return (
        <div className="table-wrapper-card">
            {/* Table Control Bar */}
            <div className="table-controls">
                <div className="filters-group">
                    <div className="filter-item">
                        <label>Status:</label>
                        <select
                            value={statusFilter || "ALL"}
                            onChange={(e) => onStatusFilterChange(e.target.value === "ALL" ? "" : e.target.value)}
                        >
                            {STATUSES.map((st) => (
                                <option key={st} value={st}>{st}</option>
                            ))}
                        </select>
                    </div>

                    <div className="filter-item">
                        <label>Category:</label>
                        <select
                            value={categoryFilter || "ALL"}
                            onChange={(e) => onCategoryFilterChange(e.target.value === "ALL" ? "" : e.target.value)}
                        >
                            {CATEGORIES.map((cat) => (
                                <option key={cat} value={cat}>{cat}</option>
                            ))}
                        </select>
                    </div>
                </div>

                <button onClick={onOpenAddModal} className="btn-primary">
                    + Add Subscription
                </button>
            </div>

            {/* Table / Empty State */}
            {subscriptions.length === 0 ? (
                <div className="table-empty-state">
                    <div className="empty-icon">💳</div>
                    <h4>No Subscriptions Found</h4>
                    <p>There are no subscription records matching your current filter.</p>
                    <button onClick={onOpenAddModal} className="btn-secondary" style={{ marginTop: "1rem" }}>
                        Create Your First Subscription
                    </button>
                </div>
            ) : (
                <div className="table-scroll-container">
                    <table className="saas-table">
                        <thead>
                            <tr>
                                <th>Name</th>
                                <th>Category</th>
                                <th>Price</th>
                                <th>Billing Cycle</th>
                                <th>Status</th>
                                <th>Last Used</th>
                                <th>Created</th>
                                <th>Actions</th>
                            </tr>
                        </thead>
                        <tbody>
                            {subscriptions.map((sub) => (
                                <tr key={sub.id}>
                                    <td className="font-semibold text-main">{sub.name}</td>
                                    <td>
                                        <span className="category-pill">{sub.category}</span>
                                    </td>
                                    <td className="font-semibold">${Number(sub.price).toFixed(2)}</td>
                                    <td className="text-muted">{sub.billingCycle}</td>
                                    <td>
                                        <span className={`summary-card-badge badge-${STATUS_BADGE_MAP[sub.status] || "neutral"}`}>
                                            {sub.status}
                                        </span>
                                    </td>
                                    <td className="text-muted">{formatDate(sub.lastUsedAt)}</td>
                                    <td className="text-muted">{formatDate(sub.createdAt)}</td>
                                    <td className="actions-cell">
                                        <button
                                            onClick={() => onMarkUsed(sub.id)}
                                            className="action-btn action-mark"
                                            title="Mark subscription as used today"
                                        >
                                            ⚡ Mark Used
                                        </button>
                                        <button
                                            onClick={() => onEdit(sub)}
                                            className="action-btn action-edit"
                                            title="Edit subscription"
                                        >
                                            ✏️ Edit
                                        </button>
                                        <button
                                            onClick={() => onDelete(sub)}
                                            className="action-btn action-delete"
                                            title="Delete subscription"
                                        >
                                            🗑️ Delete
                                        </button>
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            )}

            {/* Pagination Controls */}
            {pageInfo && pageInfo.totalPages > 1 && (
                <div className="table-pagination">
                    <span className="pagination-info">
                        Showing page {pageInfo.pageNumber + 1} of {pageInfo.totalPages} ({pageInfo.totalElements} total items)
                    </span>
                    <div className="pagination-buttons">
                        <button
                            onClick={() => onPageChange(pageInfo.pageNumber - 1)}
                            disabled={pageInfo.pageNumber === 0}
                            className="btn-pagination"
                        >
                            &laquo; Previous
                        </button>
                        <button
                            onClick={() => onPageChange(pageInfo.pageNumber + 1)}
                            disabled={pageInfo.last}
                            className="btn-pagination"
                        >
                            Next &raquo;
                        </button>
                    </div>
                </div>
            )}
        </div>
    );
}

export default SubscriptionTable;
