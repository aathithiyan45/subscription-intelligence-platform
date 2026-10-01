function DeleteConfirmModal({ isOpen, onClose, onConfirm, subscriptionName, isDeleting = false }) {
    if (!isOpen) return null;

    return (
        <div className="modal-backdrop">
            <div className="modal-container modal-sm">
                <div className="modal-header">
                    <h3>Delete Subscription</h3>
                    <button onClick={onClose} className="modal-close-btn">&times;</button>
                </div>

                <div className="modal-body">
                    <p>
                        Are you sure you want to delete <strong>{subscriptionName || "this subscription"}</strong>?
                    </p>
                    <p className="modal-subtext">This action cannot be undone.</p>
                </div>

                <div className="modal-actions">
                    <button onClick={onClose} className="btn-secondary" disabled={isDeleting}>
                        Cancel
                    </button>
                    <button onClick={onConfirm} className="btn-danger" disabled={isDeleting}>
                        {isDeleting ? "Deleting..." : "Delete Subscription"}
                    </button>
                </div>
            </div>
        </div>
    );
}

export default DeleteConfirmModal;
