import http from "k6/http";
import {check, sleep} from "k6";

// Test configuration
export const options = {
    thresholds: {
        // Assert that 99% of requests finish within 3000ms.
        http_req_duration: ["p(99) < 3000"],
    },
    // Ramp the number of virtual users up and down
    stages: [
        {duration: "4s", target: 1000},
        {duration: "20s", target: 1000},
        {duration: "4s", target: 0},
    ],
};

function getRandomInt(max) {
    return Math.floor(Math.random() * max);
}

// Simulated user behavior
export default function () {
    let data = {
        merchantId: "2a3e59ff-b549-4ca2-979c-e771c117f350",
        customerId: "client-" + getRandomInt(1000),
        orderId: getRandomInt(1000000),
        currency: "USDT-TRC20",
        amount: Math.random() * 10000000 + 1,
        successUrl: "http://ya.ru",
        failUrl: "http://google.com",
        description: "some"
    }
    let res = http.post("http://localhost:8046/invoice", JSON.stringify(data), {
        headers: {'Content-Type': 'application/json'},
    });

    // Validate response status
    check(res, {
        "status was 201": (r) => {
            return r.status == 201
        }
    });
    sleep(0.2);
}
