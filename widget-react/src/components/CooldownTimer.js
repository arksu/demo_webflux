import {useEffect, useState} from "react";

function CooldownTimer({deadline}) {
    const calculateTimeLeft = () => {
        console.log("calculateTimeLeft")
        const difference = +new Date(deadline) - +new Date();

        if (difference > 0) {
            const hours = Math.floor(difference / (1000 * 60 * 60))
            const minutes = Math.floor((difference / 1000 / 60) % 60)
            const seconds = Math.floor((difference / 1000) % 60)

            const str = hours.toString().padStart(2, '0') + ":" +
                minutes.toString().padStart(2, '0') + ":" +
                seconds.toString().padStart(2, '0')
            console.log("timeLeft", str)
            return str;
        } else {
            return "00:00:00"
        }
    };

    const [timeLeft, setTimeLeft] = useState(calculateTimeLeft());

    useEffect(() => {
        const timer = setInterval(() => {
            console.log("111")
            setTimeLeft(calculateTimeLeft());
        }, 1000);

        // Clear timeout if the component is unmounted
        return () => clearInterval(timer);
    });

    return (
        <>{timeLeft}</>
    )

}

export default CooldownTimer;
