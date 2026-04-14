import { useAuth } from "@/context/AuthContext";
import { useState } from "react";
import { useForm, SubmitHandler } from "react-hook-form";
import toast from "react-hot-toast";
import { useNavigate } from "react-router";


type FormData = {
  username: string
  password: string
}

export default function LoginPage() {
  const [isRegister, setIsRegister] = useState(false);
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<FormData>();

  const { login, register: signUp } = useAuth();
  const navigate = useNavigate();

  const onSubmit: SubmitHandler<FormData> = async (data) => {
    try {
        if(isRegister) {
            await signUp(data.username, data.password);
            toast.success("Account created!");
        } else {
            await login(data.username, data.password);
            toast.success("Welcome back");
        }
        navigate("/");
    } catch (e) {
        const message = e instanceof Error ? e.message : "Something went wrong";
        toast.error(message);
    }
  }

  return (
    <div>
        <h1>{isRegister ? "Register" : "Login"}</h1>
        <form onSubmit={handleSubmit(onSubmit)}>
            <label>username</label>
            <input {...register("username", { required: "Username required" })} />
            {errors.username && <span>{errors.username.message}</span>}

            <label>password</label>
            <input type="password" {...register("password", { required: "Password Required", minLength: { value: 8, message: "Min 8 characters"} })} />
            {errors.password && <span>{errors.password.message}</span>}
            
            <button type="submit" disabled={isSubmitting}>
                {isRegister ? "Register" : "Login"}
            </button>
        </form>

        <button onClick={() => setIsRegister(!isRegister)}>
            {isRegister ? "Have an account? Login" : "New user? Register"}
        </button>
    </div>
  )
}