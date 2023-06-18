import {Dropdown} from "react-bootstrap";
import {useEffect} from "react";
import {useTranslation} from "react-i18next";

function LanguageSelector() {
    const {i18n} = useTranslation();
    const changeLanguage = (language) => {
        i18n.changeLanguage(language);
        localStorage.setItem('language', language);
    };

    useEffect(() => {
        const savedLanguage = localStorage.getItem('language');
        if (savedLanguage) {
            i18n.changeLanguage(savedLanguage);
        }
    }, [i18n])

    return (
        <Dropdown drop="up" align="end" onSelect={changeLanguage}>
            <Dropdown.Toggle dir="down" variant="secondary">
                {i18n.language.toUpperCase()}
            </Dropdown.Toggle>
            <Dropdown.Menu>
                <Dropdown.Item eventKey="en">EN</Dropdown.Item>
                <Dropdown.Item eventKey="ru">RU</Dropdown.Item>
            </Dropdown.Menu>
        </Dropdown>
    )
}

export default LanguageSelector
