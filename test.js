import http from "k6/http";
import { check, sleep } from "k6";

// Test configuration
export const options = {
  thresholds: {
    // Assert that 99% of requests finish within 3000ms.
    http_req_duration: ["p(99) < 3000"],
  },
  // Ramp the number of virtual users up and down
  stages: [
    { duration: "5s", target: 100 },
    { duration: "20s", target: 100 },
    { duration: "5s", target: 0 },
  ],
};

// Simulated user behavior
export default function () {
  let res = http.get("http://localhost:8046/account/a1f18428-cc07-4c4b-8cb2-bbf86ce8d6d7");
  // Validate response status
  check(res, { "status was 200": (r) => r.status == 200 });
  sleep(0.2);
}
