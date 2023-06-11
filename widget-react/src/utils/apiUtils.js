import {toast} from "react-toastify";

export function getApiErrorText(e) {
    console.error(e)

    let errorText = e.message
    if (e.response) {
        errorText = e.response.data.message
        if (!errorText) {
            if (e.response.data) {
                errorText = "[" + e.response.status + "]" + JSON.stringify(e.response.data)
            } else {
                errorText = e.message
                errorText += " " + e.response.statusText
            }
        }
    }
    console.log("errorText", errorText)
    return errorText
}

export function handleApiError(e) {
    showError(e.response.status + ": " + getApiErrorText(e))
}

export function showError(msg) {
    toast.error(msg, {
        autoClose: 5000,
        hideProgressBar: false,
        closeOnClick: true,
        pauseOnHover: true,
        draggable: true,
        theme: "colored",
    });
}
