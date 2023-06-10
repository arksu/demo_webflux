import {Link, useParams} from "react-router-dom";
import {Button, Col, Container, Dropdown, Row, Spinner} from "react-bootstrap";

import axios from "axios";
import {useEffect, useState} from "react";
import {getApiErrorText, handleApiError, showError} from "../utils/apiUtils";
import {useTranslation} from "react-i18next";
import QRCode from "react-qr-code";

function Widget() {
    let {id} = useParams()
    const {t} = useTranslation()
    const [invoice, setInvoice] = useState(null);
    const [availableCurrencies, setAvailableCurrencies] = useState(null);
    const [error, setError] = useState(null);
    const [sendingSelectCurrency, setSendingSelectCurrency] = useState(false)
    const [pendingTimer, setPendingTimer] = useState(undefined)

    function startPendingTimer() {
        console.log("startPendingTimer")
        if (pendingTimer) {
            clearInterval(pendingTimer)
            setPendingTimer(undefined)
        }
        let timer = setInterval(() => {
            // тут логика подсчета оставшегося времени на оплату
            console.log("timer")
        }, 1000)
        setPendingTimer(timer)
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
                // если счет новый - надо загрузить список доступных валют
                if (response.data.status === 'NEW') {

                    // TODO debug
                    setTimeout(() => {
                        fetchAvailableCurrencies();
                    }, 300)
                }
                console.log(response.data)
                setInvoice(response.data)
                // если счет в ожидании оплаты - надо запустить таймер
                if (response.data.status === 'PENDING' || response.data.status === 'NOT_ENOUGH') {
                    startPendingTimer()
                }
            } catch (error) {
                setError(getApiErrorText(error))
            }
        }

        console.log("call fetchInvoice")

        // TODO debug
        setTimeout(() => {
            fetchInvoice();
        }, 300)
    }, [id]);

    if (error) {
        return <div>Error: {error}</div>;
    }
    if (!invoice) {
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
            setInvoice(response.data)
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

    function getWidget() {
        if (invoice.status === 'NEW') {
            const list = availableCurrencies ? availableCurrencies.list.map(v =>
                <div key={v.name} className="d-grid gap-2">
                    <Button className="mb-2" disabled={sendingSelectCurrency}
                            onClick={() => selectCurrency(v.name)}>{v.name} : {v.amount}
                    </Button>
                </div>
            ) : null
            return <>
                <Row className="mb-3">
                    <Col xs={4} className="">
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
                        <Row className="text-center">
                            <Col>
                                Please select a currency to pay
                            </Col>
                        </Row>
                        {list}
                    </>}
            </>

        } else if (invoice.status === 'PENDING') {
            return <>
                <Row>
                    <Col xs={4} className="">
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
                    {t('main:address')}
                </Row>
                <Row className="fs-6 fw-bold justify-content-center text-break">
                    {invoice.walletAddress}
                </Row>
            </>
        } else if (invoice.status === 'NOT_ENOUGH') {
            return <>
            </>
        }
    }

    return <Container>
        <Row className="justify-content-center">
            <Col xs={9} sm={8} md={6} lg={5} xl={4} xxl={3}
                 className="shadow-lg border border-primary widget-container-row">
                <Row>
                    <Col>
                        Waiting payment...
                    </Col>
                    <Col className="text-end fw-bold">
                        00:12:54
                    </Col>
                </Row>
                <hr/>
                {getWidget()}
                <hr/>
                <Row className="align-items-center">
                    <Col>
                        <Link to="">Help link</Link>
                    </Col>
                    <Col className="text-end">
                        <Dropdown drop="up" align="end">
                            <Dropdown.Toggle dir="down">EN</Dropdown.Toggle>
                            <Dropdown.Menu>
                                <Dropdown.Item>EN</Dropdown.Item>
                                <Dropdown.Item>RU</Dropdown.Item>
                            </Dropdown.Menu>
                        </Dropdown>
                    </Col>
                </Row>
            </Col>
        </Row>
    </Container>
}

export default Widget
