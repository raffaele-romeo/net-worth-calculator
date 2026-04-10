import { useState } from "react";

export function Counter() {
    const [isRegister, setIsRegister] = useState(false);

    return (
        <div>
            <p>Is Register: {String(isRegister)}</p>
            <button onClick={ () => setIsRegister(true) }>Register</button>
            <button onClick={ () => setIsRegister(false) }>Reset</button>
        </div>
    );
}