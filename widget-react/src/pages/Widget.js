import {useParams} from "react-router-dom";
import {Button, Spinner} from "react-bootstrap";

import axios from "axios";
import {useEffect, useState} from "react";
import {getApiErrorText, handleApiError, showError} from "../utils/apiUtils";

function Widget() {
    let {id} = useParams()
    const [data, setData] = useState(null);
    const [availableCurrencies, setAvailableCurrencies] = useState(null);
    const [error, setError] = useState(null);
    const [sendingSelectCurrency, setSendingSelectCurrency] = useState(false)

    function startPendingTimer() {
        setInterval(() => {
            console.log("timer")
        }, 1000)
    }

    useEffect(() => {
        const fetchAvailableCurrencies = async () => {
            try {
                const response = await axios.get(`${process.env.REACT_APP_API_URL}/invoice/${id}/available`);
                console.log("list", response.data)
                setAvailableCurrencies(response.data)
            } catch (error) {
                handleApiError(error)
            }
        }

        const fetchInvoice = async () => {
            try {
                const response = await axios.get(`${process.env.REACT_APP_API_URL}/invoice/${id}`);
                if (response.data.status === 'NEW') {
                    setTimeout(() => {
                        fetchAvailableCurrencies();
                    }, 300)
                    // fetchAvailableCurrencies()
                }
                console.log(response.data)
                setData(response.data)
                if (response.data.status === 'PENDING' || response.data.status === 'NOT_ENOUGH') {
                    startPendingTimer()
                }
            } catch (error) {
                setError(getApiErrorText(error))
            }
        }

        console.log("call fetchInvoice")
        setTimeout(() => {
            fetchInvoice();
        }, 300)
    }, [id]);

    if (error) {
        return <div>Error: {error}</div>;
    }
    if (!data) {
        return <div>Loading...
            <Spinner size="sm" animation="border" role="status"/>
        </div>
    }


    const selectCurrency = async (name) => {
        console.log("selectCurrency", name)
        setSendingSelectCurrency(true)
        try {
            const response = await axios.post(`${process.env.REACT_APP_API_URL}/order`, {
                invoiceId: id,
                selectedCurrency: name
            });
            console.log(response.data)
            setData(response.data)
            startPendingTimer()
        } catch (error) {
            if (error.response && error.response.data.code === 'NO_FREE_WALLET') {
                showError(error.response.data.message)
            } else {
                handleApiError(error)
            }
        } finally {
            setSendingSelectCurrency(false)
        }
    }

    if (data.status === 'NEW') {
        const list = availableCurrencies ? availableCurrencies.list.map(v =>
            <Button disabled={sendingSelectCurrency}
                    onClick={() => selectCurrency(v.name)}
                    key={v.name}>{v.name} : {v.amount}
            </Button>
        ) : <Spinner size="sm" animation="border" role="status"/>
        return <div>
            <h1>PAYMENT</h1>
            <h2>Invoice: {data.invoiceId}</h2>
            <h2>Status: {data.status}</h2>
            <h2>Amount: {data.invoiceAmount} {data.invoiceCurrency}</h2>
            {list}
        </div>

    } else if (data.status === 'PENDING') {
        return <div>
            <h1>PAYMENT</h1>
            <h2>Invoice: {data.invoiceId}</h2>
            <h2>Status: {data.status}</h2>

            <h2>Wait amount {data.amountPending} {data.currency}</h2>
            <h2>Address: {data.walletAddress}</h2>
        </div>
    } else if (data.status === 'NOT_ENOUGH') {
        return <div>
            <h1>PAYMENT</h1>
            <h2>Invoice: {data.invoiceId}</h2>
            <h2>Status: {data.status}</h2>
        </div>
    }
}

export default Widget
