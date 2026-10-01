function Loading({ message = "Loading dashboard metrics..." }) {
    return (
        <div className="loading-container">
            <div className="loading-spinner"></div>
            <p className="loading-message">{message}</p>
        </div>
    );
}

export default Loading;
