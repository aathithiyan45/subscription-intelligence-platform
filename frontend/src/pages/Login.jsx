import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { loginUser } from "../services/authService";

function Login() {
    const navigate = useNavigate();

    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [error, setError] = useState("");
    const [loading, setLoading] = useState(false);

    const handleSubmit = async (event) => {
        event.preventDefault();

        setError("");
        setLoading(true);

        try {
            const data = await loginUser(email, password);

            localStorage.setItem("token", data.token);
            localStorage.setItem("userId", data.userId);
            localStorage.setItem("email", data.email);
            localStorage.setItem("role", data.role);

            navigate("/dashboard");
        } catch (error) {
            console.error(error);

            if (error.response?.status === 401) {
                setError("Invalid email or password");
            } else {
                setError("Unable to login. Please try again.");
            }
        } finally {
            setLoading(false);
        }
    };

    return (
        <div>
            <h1>Subscription Manager</h1>

            <h2>Login</h2>

            <form onSubmit={handleSubmit}>
                <div>
                    <label>Email</label>

                    <input
                        type="email"
                        value={email}
                        onChange={(event) => setEmail(event.target.value)}
                        placeholder="Enter your email"
                        required
                    />
                </div>

                <div>
                    <label>Password</label>

                    <input
                        type="password"
                        value={password}
                        onChange={(event) => setPassword(event.target.value)}
                        placeholder="Enter your password"
                        required
                    />
                </div>

                {error && <p>{error}</p>}

                <button type="submit" disabled={loading}>
                    {loading ? "Logging in..." : "Login"}
                </button>
            </form>
        </div>
    );
}

export default Login;