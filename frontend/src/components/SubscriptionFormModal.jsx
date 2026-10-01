import { useState, useEffect } from "react";

const BILLING_CYCLES = ["WEEKLY", "MONTHLY", "QUARTERLY", "YEARLY"];
const CATEGORIES = ["ENTERTAINMENT", "SOFTWARE", "UTILITIES", "FITNESS", "WORK", "EDUCATION", "OTHER"];
const STATUSES = ["ACTIVE", "PAUSED", "CANCELLED", "EXPIRED"];

function SubscriptionFormModal({ isOpen, onClose, onSubmit, initialData = null, isSubmitting = false }) {
    const [name, setName] = useState("");
    const [price, setPrice] = useState("");
    const [billingCycle, setBillingCycle] = useState("MONTHLY");
    const [category, setCategory] = useState("SOFTWARE");
    const [status, setStatus] = useState("ACTIVE");
    const [lastUsedAt, setLastUsedAt] = useState("");
    const [validationError, setValidationError] = useState("");

    useEffect(() => {
        if (initialData) {
            setName(initialData.name || "");
            setPrice(initialData.price != null ? String(initialData.price) : "");
            setBillingCycle(initialData.billingCycle || "MONTHLY");
            setCategory(initialData.category || "SOFTWARE");
            setStatus(initialData.status || "ACTIVE");
            
            // Format ISO datetime string to datetime-local input string
            if (initialData.lastUsedAt) {
                const dt = new Date(initialData.lastUsedAt);
                if (!isNaN(dt.getTime())) {
                    const isoLocal = dt.toISOString().slice(0, 16);
                    setLastUsedAt(isoLocal);
                } else {
                    setLastUsedAt("");
                }
            } else {
                setLastUsedAt("");
            }
        } else {
            // Reset for new form
            setName("");
            setPrice("");
            setBillingCycle("MONTHLY");
            setCategory("SOFTWARE");
            setStatus("ACTIVE");
            setLastUsedAt("");
        }
        setValidationError("");
    }, [initialData, isOpen]);

    if (!isOpen) return null;

    const handleSubmit = (e) => {
        e.preventDefault();
        setValidationError("");

        if (!name.trim()) {
            setValidationError("Subscription name is required");
            return;
        }

        const numericPrice = parseFloat(price);
        if (isNaN(numericPrice) || numericPrice <= 0) {
            setValidationError("Price must be a positive number");
            return;
        }

        const formData = {
            name: name.trim(),
            price: numericPrice,
            billingCycle,
            category,
            status,
            lastUsedAt: lastUsedAt ? new Date(lastUsedAt).toISOString() : null,
        };

        onSubmit(formData);
    };

    return (
        <div className="modal-backdrop">
            <div className="modal-container">
                <div className="modal-header">
                    <h3>{initialData ? "Edit Subscription" : "Add New Subscription"}</h3>
                    <button onClick={onClose} className="modal-close-btn">&times;</button>
                </div>

                <form onSubmit={handleSubmit} className="modal-form">
                    {validationError && <div className="form-error">{validationError}</div>}

                    <div className="form-group">
                        <label htmlFor="sub-name">Subscription Name *</label>
                        <input
                            id="sub-name"
                            type="text"
                            value={name}
                            onChange={(e) => setName(e.target.value)}
                            placeholder="e.g. Netflix, GitHub, Spotify"
                            required
                        />
                    </div>

                    <div className="form-row">
                        <div className="form-group flex-1">
                            <label htmlFor="sub-price">Price ($) *</label>
                            <input
                                id="sub-price"
                                type="number"
                                step="0.01"
                                min="0.01"
                                value={price}
                                onChange={(e) => setPrice(e.target.value)}
                                placeholder="14.99"
                                required
                            />
                        </div>

                        <div className="form-group flex-1">
                            <label htmlFor="sub-cycle">Billing Cycle *</label>
                            <select
                                id="sub-cycle"
                                value={billingCycle}
                                onChange={(e) => setBillingCycle(e.target.value)}
                                required
                            >
                                {BILLING_CYCLES.map((cycle) => (
                                    <option key={cycle} value={cycle}>{cycle}</option>
                                ))}
                            </select>
                        </div>
                    </div>

                    <div className="form-row">
                        <div className="form-group flex-1">
                            <label htmlFor="sub-category">Category *</label>
                            <select
                                id="sub-category"
                                value={category}
                                onChange={(e) => setCategory(e.target.value)}
                                required
                            >
                                {CATEGORIES.map((cat) => (
                                    <option key={cat} value={cat}>{cat}</option>
                                ))}
                            </select>
                        </div>

                        <div className="form-group flex-1">
                            <label htmlFor="sub-status">Status *</label>
                            <select
                                id="sub-status"
                                value={status}
                                onChange={(e) => setStatus(e.target.value)}
                                required
                            >
                                {STATUSES.map((st) => (
                                    <option key={st} value={st}>{st}</option>
                                ))}
                            </select>
                        </div>
                    </div>

                    <div className="form-group">
                        <label htmlFor="sub-lastused">Last Used Date (Optional)</label>
                        <input
                            id="sub-lastused"
                            type="datetime-local"
                            value={lastUsedAt}
                            onChange={(e) => setLastUsedAt(e.target.value)}
                        />
                    </div>

                    <div className="modal-actions">
                        <button type="button" onClick={onClose} className="btn-secondary" disabled={isSubmitting}>
                            Cancel
                        </button>
                        <button type="submit" className="btn-primary" disabled={isSubmitting}>
                            {isSubmitting ? "Saving..." : initialData ? "Update Subscription" : "Create Subscription"}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
}

export default SubscriptionFormModal;
