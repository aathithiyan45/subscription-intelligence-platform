function ErrorMessage({ title = "An error occurred", message, onRetry }) {
    return (
        <div className="error-banner">
            <div className="error-icon">⚠️</div>
            <div className="error-content">
                <h4 className="error-title">{title}</h4>
                {message && <p className="error-text">{message}</p>}
            </div>
            {onRetry && (
                <button onClick={onRetry} className="retry-button">
                    Try Again
                </button>
            )}
        </div>
    );
}

export default ErrorMessage;
