import { useState, useEffect, useCallback } from "react";
import {
    getSubscriptions,
    createSubscription,
    updateSubscription,
    deleteSubscription,
    markSubscriptionUsed,
} from "../services/subscriptionService";
import SubscriptionTable from "../components/SubscriptionTable";
import SubscriptionFormModal from "../components/SubscriptionFormModal";
import DeleteConfirmModal from "../components/DeleteConfirmModal";
import Loading from "../components/Loading";
import ErrorMessage from "../components/ErrorMessage";

function SubscriptionsPage() {
    const [subscriptions, setSubscriptions] = useState([]);
    const [pageInfo, setPageInfo] = useState(null);
    const [page, setPage] = useState(0);
    const [statusFilter, setStatusFilter] = useState("");
    const [categoryFilter, setCategoryFilter] = useState("");
    
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [successMessage, setSuccessMessage] = useState("");

    // Modal States
    const [isFormOpen, setIsFormOpen] = useState(false);
    const [editingSub, setEditingSub] = useState(null);
    const [isDeletingOpen, setIsDeletingOpen] = useState(false);
    const [deletingSub, setDeletingSub] = useState(null);
    const [isSubmitting, setIsSubmitting] = useState(false);

    const fetchSubscriptions = useCallback(async () => {
        setLoading(true);
        setError(null);
        try {
            const userId = localStorage.getItem("userId");
            if (!userId) throw new Error("Session error: Authenticated user ID not found");

            const data = await getSubscriptions(userId, {
                page,
                size: 10,
                status: statusFilter || undefined,
                category: categoryFilter || undefined,
            });

            setSubscriptions(data.content || []);
            setPageInfo({
                pageNumber: data.pageNumber,
                pageSize: data.pageSize,
                totalElements: data.totalElements,
                totalPages: data.totalPages,
                last: data.last,
            });
        } catch (err) {
            console.error("Fetch subscriptions error:", err);
            setError(err.response?.data?.message || err.message || "Failed to load subscriptions");
        } finally {
            setLoading(false);
        }
    }, [page, statusFilter, categoryFilter]);

    useEffect(() => {
        fetchSubscriptions();
    }, [fetchSubscriptions]);

    const showSuccess = (msg) => {
        setSuccessMessage(msg);
        setTimeout(() => setSuccessMessage(""), 4000);
    };

    const handleOpenAddModal = () => {
        setEditingSub(null);
        setIsFormOpen(true);
    };

    const handleOpenEditModal = (sub) => {
        setEditingSub(sub);
        setIsFormOpen(true);
    };

    const handleOpenDeleteModal = (sub) => {
        setDeletingSub(sub);
        setIsDeletingOpen(true);
    };

    const handleFormSubmit = async (formData) => {
        setIsSubmitting(true);
        setError(null);
        try {
            const userId = localStorage.getItem("userId");
            if (editingSub) {
                await updateSubscription(userId, editingSub.id, formData);
                showSuccess(`Successfully updated "${formData.name}"`);
            } else {
                await createSubscription(userId, formData);
                showSuccess(`Successfully created subscription "${formData.name}"`);
            }
            setIsFormOpen(false);
            fetchSubscriptions();
        } catch (err) {
            console.error("Subscription save error:", err);
            setError(err.response?.data?.message || "Failed to save subscription details.");
        } finally {
            setIsSubmitting(false);
        }
    };

    const handleDeleteConfirm = async () => {
        if (!deletingSub) return;
        setIsSubmitting(true);
        setError(null);
        try {
            const userId = localStorage.getItem("userId");
            await deleteSubscription(userId, deletingSub.id);
            showSuccess(`Subscription "${deletingSub.name}" deleted.`);
            setIsDeletingOpen(false);
            setDeletingSub(null);
            fetchSubscriptions();
        } catch (err) {
            console.error("Subscription delete error:", err);
            setError(err.response?.data?.message || "Failed to delete subscription.");
        } finally {
            setIsSubmitting(false);
        }
    };

    const handleMarkUsed = async (subscriptionId) => {
        try {
            const userId = localStorage.getItem("userId");
            const updated = await markSubscriptionUsed(userId, subscriptionId);
            showSuccess(`Marked "${updated.name}" as used today!`);
            fetchSubscriptions();
        } catch (err) {
            console.error("Mark used error:", err);
            setError(err.response?.data?.message || "Failed to update last used timestamp.");
        }
    };

    return (
        <div className="subscriptions-page-container">
            {/* Banner Messages */}
            {successMessage && (
                <div className="toast-success">
                    <span>✅ {successMessage}</span>
                </div>
            )}

            {error && (
                <ErrorMessage
                    title="Subscription Error"
                    message={error}
                    onRetry={fetchSubscriptions}
                />
            )}

            {loading ? (
                <Loading message="Loading subscription records..." />
            ) : (
                <SubscriptionTable
                    subscriptions={subscriptions}
                    pageInfo={pageInfo}
                    onPageChange={(newPage) => setPage(newPage)}
                    onEdit={handleOpenEditModal}
                    onDelete={handleOpenDeleteModal}
                    onMarkUsed={handleMarkUsed}
                    statusFilter={statusFilter}
                    onStatusFilterChange={(st) => {
                        setStatusFilter(st);
                        setPage(0);
                    }}
                    categoryFilter={categoryFilter}
                    onCategoryFilterChange={(cat) => {
                        setCategoryFilter(cat);
                        setPage(0);
                    }}
                    onOpenAddModal={handleOpenAddModal}
                />
            )}

            {/* Form Modal (Add / Edit) */}
            <SubscriptionFormModal
                isOpen={isFormOpen}
                onClose={() => setIsFormOpen(false)}
                onSubmit={handleFormSubmit}
                initialData={editingSub}
                isSubmitting={isSubmitting}
            />

            {/* Delete Modal */}
            <DeleteConfirmModal
                isOpen={isDeletingOpen}
                onClose={() => setIsDeletingOpen(false)}
                onConfirm={handleDeleteConfirm}
                subscriptionName={deletingSub?.name}
                isDeleting={isSubmitting}
            />
        </div>
    );
}

export default SubscriptionsPage;
