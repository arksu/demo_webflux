import http from "k6/http";
import {check} from "k6";

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

const merchantId = "2a3e59ff-b549-4ca2-979c-e771c117f350"

function createInvoice() {
    let data = {
        merchantId: merchantId,
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
        "createInvoice status was 201": (r) => {
            return r.status === 201
        }
    });

    let result = res.json()
    return result.id
}

function createOrder(invoiceId) {
    let data = {
        invoiceId: invoiceId,
        selectedCurrency: "USDT-TRC20",
        amount: Math.random() * 10000000 + 1,
    }
    let res = http.post("http://localhost:8046/order", JSON.stringify(data), {
        headers: {'Content-Type': 'application/json'},
    });

    // Validate response status
    check(res, {
        "createOrder status was 200": (r) => {
            return r.status === 200
        }
    });
}

function debug_delay() {
    let res = http.get("http://localhost:8046/debug/delay");

    // Validate response status
    check(res, {
        "debug_delay status was 200": (r) => {
            return r.status === 200
        }
    });
}

function debug_delay2() {
    let res = http.get("http://localhost:8046/debug/delay2");

    // Validate response status
    check(res, {
        "debug_delay status was 200": (r) => {
            return r.status === 200
        }
    });
}

// Simulated user behavior
export default function () {
    let invoiceId = createInvoice()
    // sleep(0.2);
    // createOrder(invoiceId)
    // createOrder("13bcbbe2-c82c-410d-8057-44c219a0a04e")

    // debug_delay()
    // debug_delay2()
}
