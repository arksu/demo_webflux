import {Link, useParams} from "react-router-dom";
import {Button, Col, Container, Dropdown, Row, Spinner} from "react-bootstrap";

import axios from "axios";
import {useEffect, useState} from "react";
import {getApiErrorText, handleApiError, showError} from "../utils/apiUtils";
import {useTranslation} from "react-i18next";
import QRCode from "react-qr-code";
import Countdown from "react-countdown";
import LanguageSelector from "../components/LanguageSelector";

function Widget() {
    let {id} = useParams()
    const {t} = useTranslation()
    const [invoice, setInvoice] = useState(null);
    const [availableCurrencies, setAvailableCurrencies] = useState(null);
    const [error, setError] = useState(null);
    const [sendingSelectCurrency, setSendingSelectCurrency] = useState(false)

    useEffect(() => {
        const toProcess = ['NEW', 'PENDING', 'NOT_ENOUGH', 'OVERPAID']
        // если счет в ожидании оплаты - надо запустить таймер
        if (invoice && toProcess.includes(invoice.status)) {
            console.log("start timer")
            let timer = setInterval(fetchInvoice, 5000)
            return () => {
                clearInterval(timer)
            }
        }
    }, [invoice])

    const fetchAvailableCurrencies = async () => {
        try {
            const response = await axios.get(`${process.env.REACT_APP_API_URL}/invoice/${id}/available`);
            console.log("currencies list", response.data)
            setAvailableCurrencies(response.data)
        } catch (error) {
            handleApiError(error)
        }
    }

    const fetchInvoice = async () => {
        try {
            const response = await axios.get(`${process.env.REACT_APP_API_URL}/invoice/${id}`);
            // если счет новый - надо загрузить список доступных валют
            if (response.data.status === 'NEW') {

                // TODO debug
                setTimeout(() => {
                    fetchAvailableCurrencies();
                }, 300)
            }
            console.log(response.data)
            setInvoice(response.data)
            setError(null)
        } catch (error) {
            if (!invoice) {
                setError(getApiErrorText(error))
            }
        }
    }

    useEffect(() => {
        // TODO debug
        const timeout = setTimeout(() => {
            fetchInvoice();
        }, 300)

        return () => {
            clearTimeout(timeout)
        }
    }, [id]);

    if (error) {
        return <div>
            Error: {error}
        </div>;
    }
    if (!invoice) {
        return <div>
            <Spinner size="sm" animation="border" role="status" className="me-2"/>
            Loading...
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
            setInvoice(response.data)
            // startPendingTimer()
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

    function getWidget() {
        if (invoice.status === 'NEW') {
            const list = availableCurrencies ? availableCurrencies.list.map(v =>
                <div key={v.name} className="d-grid gap-2">
                    <Button className="mb-2" disabled={sendingSelectCurrency}
                            onClick={() => selectCurrency(v.name)}>{v.amount} {v.name}
                    </Button>
                </div>
            ) : null
            return <>
                <Row className="mb-3">
                    <Col xs={4} className="text-start">
                        {invoice.shopName}
                    </Col>
                    <Col xs={8} className="text-end fw-bold">
                        {invoice.invoiceAmount} {invoice.invoiceCurrency}
                    </Col>
                </Row>
                {!availableCurrencies && <>
                    <Row className="text-center">
                        <Col>
                            <Spinner size="lg" animation="border" role="status"/>
                        </Col>

                    </Row></>}
                {availableCurrencies &&
                    <>
                        <Row className="text-center mb-3">
                            <Col>
                                {t('main:select_currency')}
                            </Col>
                        </Row>
                        {list}
                    </>}
            </>

        } else if (invoice.status === 'PENDING') {
            return <>
                <Row>
                    <Col xs={4} className="text-start">
                        {invoice.shopName}
                    </Col>
                    <Col xs={8} className="text-end fw-bold">
                        {invoice.amountPending} {invoice.currency}
                    </Col>
                </Row>
                <Row className="mt-3 mb-3">
                    <QRCode
                        value={invoice.walletAddress}
                        bgColor="#dfe3e3"
                    ></QRCode>
                </Row>
                <Row className="justify-content-center">
                    {t('main:please_send')}
                </Row>
                <Row className="justify-content-center fw-bold fs-3">
                    {invoice.amountPending} {invoice.currency}
                </Row>
                <Row className="justify-content-center">
                    {t('main:to_address')}
                </Row>
                <Row className="fs-6 fw-bold justify-content-center text-break">
                    {invoice.walletAddress}
                </Row>
            </>
        } else if (invoice.status === 'COMPLETED') {
            return <>
            </>
        } else if (invoice.status === 'EXPIRED') {
            return <>
            </>
        }
    }

    return <>
        <Row>
            <Col className="text-start" xs={8}>
                {t('main:waiting_payment')}
            </Col>
            <Col className="text-end fw-bold" xs={4}>
                <Countdown date={invoice.deadline} daysInHours={true}/>
            </Col>
        </Row>
        <hr/>
        {getWidget()}
        <hr/>
        <Row className="align-items-center">
            <Col className="text-start">
                <Link to="">{t('main:help')}</Link>
            </Col>
            <Col className="text-end">
                <LanguageSelector/>
            </Col>
        </Row>
    </>
}

export default Widget
